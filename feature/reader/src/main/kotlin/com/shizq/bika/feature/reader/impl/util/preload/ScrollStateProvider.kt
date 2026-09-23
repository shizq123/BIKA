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
 * 新的内部视口契约。公开的 [ScrollStateProvider] 保持不变，旧实现可以继续只提供范围。
 */
internal interface ViewportEventProvider {
    val viewportEvents: Flow<ViewportSnapshot>
}

/**
 * 一个抽象接口，用于提供列表的滚动状态。
 */
interface ScrollStateProvider {
    /**
     * 一个 Flow，持续发射当前可见项的索引范围 (first..last)。
     * 如果列表为空或未布局，可以发射 null。
     */
    val visibleItemsRange: Flow<IntRange?>
}

/**
 * ScrollStateProvider 的 LazyListState 实现。
 */
internal class LazyListScrollStateProvider(
    private val listState: LazyListState
) : ScrollStateProvider, ViewportEventProvider {
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
        val event = ViewportSnapshot(
            visibleRange = current.range,
            direction = direction,
            cause = ViewportChangeCause.UserScroll,
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
    override val visibleItemsRange: Flow<IntRange?>
) : ScrollStateProvider, ViewportEventProvider {
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
            val event = ViewportSnapshot(
                visibleRange = current,
                direction = direction,
                // Pager 的分组变化会导致真实页码范围变化，但没有像素位移；
                // 先保守标记为 Unknown，由会话层保留已有阅读方向。
                cause = ViewportChangeCause.Unknown,
            )
            current to event
        }
        .mapNotNull { it.second }
        .distinctUntilChanged()
}
