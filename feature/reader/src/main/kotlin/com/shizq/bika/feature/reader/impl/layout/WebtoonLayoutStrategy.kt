package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.feature.reader.impl.util.preload.ViewportChangeCause
import com.shizq.bika.feature.reader.impl.util.preload.ViewportEventMarker
import kotlinx.coroutines.flow.distinctUntilChanged
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

class WebtoonLayoutStrategy(
    private val listState: LazyListState,
    private val hasPageGap: Boolean,
    private val magnifierEnabled: Boolean,
) : ReaderLayoutStrategy {

    /**
     * 条漫由**容器整体**缩放：连续滚动下逐页缩放没有意义（跨页边界会被撕开）。
     *
     * 之前这是宿主读一个 `isGestureSelfContained` 布尔标志、由宿主套上
     * `Modifier.zoomable` 的方式。改成布局自己套：缩放归属只有一个持有者，
     * 也不再需要宿主用 `key(layout::class)` 猜策略换没换来复位缩放状态——
     * zoomableState 现在随本 composable 的节点一起生灭。
     */
    @Composable
    override fun RenderContent(
        pageItems: LazyPagingItems<ChapterPage>,
        modifier: Modifier,
        onPageTap: (PageTapContext) -> Unit,
    ) {
        val zoomableState = rememberZoomableState(ZoomSpec(maxZoomFactor = 4f))
        // 视口 = LazyColumn 自身。与翻页模式用同一个 ViewportAnchor 机制，
        // 保证两条路径喂给 GestureState.calculateAction 的参考系一致。
        val viewport = remember { ViewportAnchor() }

        LazyColumn(
            state = listState,
            modifier = modifier
                .viewportAnchor(viewport)
                .zoomable(
                    state = zoomableState,
                    gestures = EnabledZoomGestures.ZoomAndPan,
                    onClick = { offset ->
                        // 容器级路径：点击坐标本就是视口局部坐标，无需换算，
                        // 只需取同一节点的尺寸。
                        val coords = viewport.coordinates ?: return@zoomable
                        if (!coords.isAttached || coords.size == IntSize.Zero) return@zoomable
                        onPageTap(PageTapContext(position = offset, viewportSize = coords.size))
                    },
                ),
            verticalArrangement = if (hasPageGap) Arrangement.spacedBy(8.dp) else Arrangement.Top
        ) {
            items(
                count = pageItems.itemCount,
                key = { index ->
                    // 部分镜像站会返回重复的 imageId，直接用 id 作 key 会触发
                    // "Key was already used" 崩溃，这里用 index+id 组合保证唯一性。
                    val page = pageItems.peek(index)
                    if (page != null) "${index}_${page.id}" else "placeholder_$index"
                },
            ) { index ->
                pageItems[index]?.let {
                    // 缩放与点击都由容器处理，这里不传 onTap
                    ComicPageItem(it, index, magnifierEnabled = magnifierEnabled)
                } ?: ChapterPageLoadStateItem(pageItems, index)
            }
        }
    }
}

class WebtoonController(
    private val listState: LazyListState,
    initialPageIndex: Int,
    private val viewportEventMarker: ViewportEventMarker = ViewportEventMarker(),
) : ReaderController {

    override val continuousScroller: ContinuousScroller = object : ContinuousScroller {
        override suspend fun scrollBy(pixels: Float): Float = listState.scrollBy(pixels)

        override val isScrollInProgress: Boolean
            get() = listState.isScrollInProgress

        override val interactionSource: InteractionSource
            get() = listState.interactionSource
    }

    /**
     * 条漫下一屏可能同时露出多页，[ReadingPositionSnapshot.first] 是进度页码，
     * [ReadingPositionSnapshot.last] 用于末页判定（最后一页很短时靠它才能触发已读完）。
     */
    override var position: ReadingPositionSnapshot by mutableStateOf(
        ReadingPositionSnapshot.single(initialPageIndex),
    )
        private set

    /**
     * [position] 的唯一写入者。
     *
     * `lastValidIndex` 这种「取不到就沿用上次」的逻辑必须有唯一写入点，也因此
     * 不能放在 derivedStateOf 里（那里不允许写外部状态）。之前它是
     * `snapshotFlow { calculateCurrentPageIndex() }` 里的副作用，而那个冷流被
     * 四处独立 collect，等于四个协程并发读写同一个字段。
     */
    override suspend fun track() {
        snapshotFlow { readLayoutSnapshot() }
            .distinctUntilChanged()
            .collect { info ->
                val resolved = resolveListReadingPosition(
                    layoutInfo = info,
                    lastValidIndex = position.first,
                )
                position = ReadingPositionSnapshot(
                    first = resolved,
                    last = info.lastVisibleItem?.index?.coerceAtLeast(resolved) ?: resolved,
                )
            }
    }

    override suspend fun scrollNextPage() {
        viewportEventMarker.mark(ViewportChangeCause.ProgrammaticJump)
        val viewportHeight = listState.layoutInfo.viewportSize.height
        // 如果布局还未完成，直接返回
        if (viewportHeight == 0) return

        val scrollDistance = viewportHeight * 0.8f
        listState.animateScrollBy(scrollDistance)
    }

    override suspend fun scrollPrevPage() {
        viewportEventMarker.mark(ViewportChangeCause.ProgrammaticJump)
        val viewportHeight = listState.layoutInfo.viewportSize.height
        if (viewportHeight == 0) return

        val scrollDistance = viewportHeight * 0.8f
        listState.animateScrollBy(-scrollDistance)
    }

    override suspend fun scrollToPage(index: Int) {
        viewportEventMarker.mark(ViewportChangeCause.ProgrammaticJump)
        // 不能用 layoutInfo.totalItemsCount 做 clamp：它在布局后才会更新，可能滞后于
        // paging 的 itemCount，导致目标页被 clamp 到已布局末尾、滚动落空。
        // scrollToItem 对超界 index 会滚动到末尾，调用方应确保数据已加载到目标页。
        listState.scrollToItem(index.coerceAtLeast(0))
    }

    /**
     * 把 Compose 的 [LazyListState.layoutInfo] 转成判定规则需要的快照。
     * 判定规则本身见 [resolveListReadingPosition]（纯函数，可单测）。
     */
    private fun readLayoutSnapshot(): ListReadingLayoutInfo {
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        return ListReadingLayoutInfo(
            firstVisibleItem = visibleItems.firstOrNull()?.let {
                VisibleItemSnapshot(index = it.index, offset = it.offset, size = it.size)
            },
            lastVisibleItem = visibleItems.lastOrNull()?.let {
                VisibleItemSnapshot(index = it.index, offset = it.offset, size = it.size)
            },
            totalItemsCount = layoutInfo.totalItemsCount,
            viewportEndOffset = layoutInfo.viewportEndOffset,
        )
    }
}
