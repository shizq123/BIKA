package com.shizq.bika.feature.reader.impl.util.preload

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.runningFold

/** 视口变化的来源，用于避免把程序行为误判成用户滚动。 */
enum class ViewportChangeCause {
    UserScroll,
    ProgrammaticJump,
    LayoutReflow,
    DataRefresh,
    Unknown,
}

enum class ScrollDirection {
    Forward,
    Backward,
}

data class ViewportSnapshot(
    val visibleRange: IntRange?,
    val direction: ScrollDirection? = null,
    val cause: ViewportChangeCause = ViewportChangeCause.Unknown,
    val generation: Long = 0L,
)

/**
 * Controller 在执行跳页等程序行为前写入标记，状态提供者在下一次视口变化时消费。
 * 标记与对应的滚动状态由同一 UI 线程访问，不需要额外同步。
 */
class ViewportEventMarker {
    private var pendingCause: ViewportChangeCause? = null
    private var generation: Long = 0L

    fun mark(
        cause: ViewportChangeCause,
        generation: Long? = null,
        advanceGeneration: Boolean = false,
    ) {
        pendingCause = cause
        when {
            generation != null -> this.generation = maxOf(this.generation, generation)
            advanceGeneration -> this.generation++
        }
    }

    fun snapshot(
        visibleRange: IntRange?,
        direction: ScrollDirection?,
        defaultCause: ViewportChangeCause,
    ): ViewportSnapshot {
        val cause = pendingCause ?: defaultCause
        pendingCause = null
        return ViewportSnapshot(
            visibleRange = visibleRange,
            direction = direction,
            cause = cause,
            generation = generation,
        )
    }
}

interface ScrollStateProvider {
    /**
     * 一个 Flow，持续发射当前可见项的索引范围 (first..last)。
     * 如果列表为空或未布局，可以发射 null。
     */
    val visibleItemsRange: Flow<IntRange?>

    /**
     * 包含方向、变化原因和 generation 的完整视口事件。
     *
     * 只实现 [visibleItemsRange] 的提供者会自动退化为 Unknown 类型的视口事件；
     * 阅读器内置实现应覆盖此属性，以保留滚动方向和程序跳转等信息。
     */
    val viewportEvents: Flow<ViewportSnapshot>
        get() = visibleItemsRange.map { ViewportSnapshot(visibleRange = it) }
}

/**
 * ScrollStateProvider 的 LazyListState 实现。
 */
internal class LazyListScrollStateProvider(
    private val listState: LazyListState,
    internal val eventMarker: ViewportEventMarker = ViewportEventMarker(),
) : ScrollStateProvider {
    private data class RawViewport(
        val range: IntRange?,
        val firstIndex: Int?,
        val firstOffset: Int,
    )

    override val viewportEvents: Flow<ViewportSnapshot> = snapshotFlow {
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val first = visibleItems.firstOrNull()
        val last = visibleItems.lastOrNull()
        RawViewport(
            range = if (first != null && last != null) first.index..last.index else null,
            firstIndex = first?.index,
            firstOffset = first?.offset ?: 0,
        )
    }.runningFold(
        initial = Pair<RawViewport?, ViewportSnapshot?>(null, null),
    ) { state, current ->
        val previous = state.first
        val previousEvent = state.second
        val direction = when {
            previous == null || current.firstIndex == null || previous.firstIndex == null -> null
            current.firstIndex > previous.firstIndex -> ScrollDirection.Forward
            current.firstIndex < previous.firstIndex -> ScrollDirection.Backward
            current.firstOffset < previous.firstOffset -> ScrollDirection.Forward
            current.firstOffset > previous.firstOffset -> ScrollDirection.Backward
            else -> previousEvent?.direction
        }
        val event = eventMarker.snapshot(
            visibleRange = current.range,
            direction = direction,
            defaultCause = ViewportChangeCause.UserScroll,
        )
        current to event
    }.mapNotNull { it.second }.distinctUntilChanged()

    override val visibleItemsRange: Flow<IntRange?> = viewportEvents
        .map { it.visibleRange }
        .distinctUntilChanged()
}

/**
 * 翻页模式的实现：接收一个已经换算成**真实页码**范围的 Flow。
 *
 * 不直接持有 PagerState 是因为 `pagerState.currentPage` 是「翻页单位」下标而不是
 * 页码。跨页模式一屏两页，按 currentPage 预载会漏掉右页；有宽页独占一屏时
 * 单位下标与页码的偏移还会逐渐累积。换算依赖跨页分组
 * （[com.shizq.bika.feature.reader.impl.layout.PageSpreadState]），
 * 由 layout 包算好后传入，避免 preload 包反向依赖 layout 包。
 *
 * 单页模式同样走这里：分组会退化成每页一个单位，行为与直接用 currentPage 一致。
 */
internal class SpreadScrollStateProvider(
    override val visibleItemsRange: Flow<IntRange?>,
    internal val eventMarker: ViewportEventMarker = ViewportEventMarker(),
) : ScrollStateProvider {
    override val viewportEvents: Flow<ViewportSnapshot> = visibleItemsRange
        .runningFold(
            initial = Pair<IntRange?, ViewportSnapshot?>(null, null),
        ) { state, current ->
            val previousRange = state.first
            val direction = when {
                previousRange == null || current == null -> null
                current.first > previousRange.first -> ScrollDirection.Forward
                current.first < previousRange.first -> ScrollDirection.Backward
                else -> state.second?.direction
            }
            val event = eventMarker.snapshot(
                visibleRange = current,
                direction = direction,
                defaultCause = ViewportChangeCause.UserScroll,
            )
            current to event
        }
        .mapNotNull { it.second }
        .distinctUntilChanged()
}
