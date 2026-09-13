package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.data.paging.ChapterPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class WebtoonLayoutStrategy(
    private val listState: LazyListState,
    private val hasPageGap: Boolean,
    private val magnifierEnabled: Boolean,
) : ReaderLayoutStrategy {
    /** 条漫由容器整体缩放：连续滚动下逐页缩放没有意义。 */
    override val isGestureSelfContained: Boolean = false

    @Composable
    override fun RenderContent(
        pageItems: LazyPagingItems<ChapterPage>,
        modifier: Modifier,
        onPageTap: (PageTapContext) -> Unit,
    ) {
        LazyColumn(
            state = listState,
            modifier = modifier,
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
    initialPageIndex: Int
) : ReaderController {
    
    private var lastValidIndex: Int = initialPageIndex

    override val continuousScroller: ContinuousScroller = object : ContinuousScroller {
        override suspend fun scrollBy(pixels: Float): Float = listState.scrollBy(pixels)

        override val isScrollInProgress: Boolean
            get() = listState.isScrollInProgress

        override val interactionSource: InteractionSource
        get() = listState.interactionSource
    }

    override val visibleItemIndex: Flow<Int> = snapshotFlow {
        calculateCurrentPageIndex()
    }.distinctUntilChanged()

    override suspend fun scrollNextPage() {
        val viewportHeight = listState.layoutInfo.viewportSize.height
        // 如果布局还未完成，直接返回
        if (viewportHeight == 0) return

        val scrollDistance = viewportHeight * 0.8f
        listState.animateScrollBy(scrollDistance)
    }

    override suspend fun scrollPrevPage() {
        val viewportHeight = listState.layoutInfo.viewportSize.height
        if (viewportHeight == 0) return

        val scrollDistance = viewportHeight * 0.8f
        listState.animateScrollBy(-scrollDistance)
    }

    override suspend fun scrollToPage(index: Int) {
        // 不能用 layoutInfo.totalItemsCount 做 clamp：它在布局后才会更新，可能滞后于
        // paging 的 itemCount，导致目标页被 clamp 到已布局末尾、滚动落空。
        // scrollToItem 对超界 index 会滚动到末尾，调用方应确保数据已加载到目标页。
        listState.scrollToItem(index.coerceAtLeast(0))
    }

    /**
     * 计算当前阅读到的页码（用于进度保存）。判定规则见 [resolveListReadingPosition]，
     * 这里只负责把 Compose 的 [LazyListState.layoutInfo] 转成规则需要的快照。
     */
    private fun calculateCurrentPageIndex(): Int {
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo

        val result = resolveListReadingPosition(
            layoutInfo = ListReadingLayoutInfo(
                firstVisibleItem = visibleItems.firstOrNull()?.let {
                    VisibleItemSnapshot(index = it.index, offset = it.offset, size = it.size)
                },
                lastVisibleItem = visibleItems.lastOrNull()?.let {
                    VisibleItemSnapshot(index = it.index, offset = it.offset, size = it.size)
                },
                totalItemsCount = layoutInfo.totalItemsCount,
                viewportEndOffset = layoutInfo.viewportEndOffset,
            ),
            lastValidIndex = lastValidIndex,
        )
        lastValidIndex = result
        return result
    }
}