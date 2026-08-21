package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
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

class WebtoonLayout(
    private val listState: LazyListState,
    private val hasPageGap: Boolean
) : ReaderLayout {
    @Composable
    override fun Content(
        pageItems: LazyPagingItems<ChapterPage>,
        modifier: Modifier,
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
                    ComicPageItem(it, index)
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

    override val totalPages: Int
        get() = listState.layoutInfo.totalItemsCount
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

    override suspend fun scrollBy(value: Float) {
        listState.scrollBy(value)
    }

    /**
     * 计算当前阅读到的页码（用于进度保存）。
     *
     * 规则：
     * 1. 如果滚动到底部，且最后一项完全可见，强制视为最后一页（解决最后一页较短时无法触发已读的问题）。
     * 2. 否则取**第一个已经开始进入视口的 item**（firstVisibleItemIndex），
     *    这代表用户当前正在阅读的起始页。
     *
     * 为何不用视口中心线：
     * - 条漫图片可能极高（超过屏幕高度数倍），用户已滚动到第28页顶部，
     *   但中心线仍指向第12页，导致进度保存为第12页，与用户感知严重偏差。
     * - 使用 firstVisibleItemIndex 可确保进度不落后于用户已看到的内容。
     *   即使第一个可见页尚未看完，下次也只是从该页开始，不会丢失已读内容。
     */
    private fun calculateCurrentPageIndex(): Int {
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo

        if (visibleItems.isEmpty() || layoutInfo.totalItemsCount == 0) return lastValidIndex

        val lastVisibleItem = visibleItems.last()

        // 判定是否到底：最后一项可见且底部在视口内
        val isLastItemVisible = lastVisibleItem.index == layoutInfo.totalItemsCount - 1
        if (isLastItemVisible) {
            val isBottomEdgeVisible =
                (lastVisibleItem.offset + lastVisibleItem.size) <= layoutInfo.viewportEndOffset
            if (isBottomEdgeVisible) {
                lastValidIndex = lastVisibleItem.index
                return lastValidIndex
            }
        }

        // 取第一个进入视口的 item（firstVisibleItemIndex）
        lastValidIndex = visibleItems.first().index
        return lastValidIndex
    }
}