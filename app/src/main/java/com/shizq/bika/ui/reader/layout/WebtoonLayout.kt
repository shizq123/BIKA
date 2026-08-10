package com.shizq.bika.ui.reader.layout

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
import com.shizq.bika.paging.ChapterPage
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
) : ReaderController {
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
        val total = listState.layoutInfo.totalItemsCount
        if (total > 0) {
            listState.scrollToItem(index.coerceIn(0, total - 1))
        } else {
            listState.scrollToItem(index.coerceAtLeast(0))
        }
    }

    override suspend fun scrollBy(value: Float) {
        listState.scrollBy(value)
    }

    /**
     * 计算当前阅读到的页码
     * 规则：
     * 1. 如果滚动到底部，且最后一项完全可见，强制视为最后一页（解决最后一页较短时无法触发已读的问题）。
     * 2. 否则取**视口中心线所在的项**——即用户实际正在阅读的那一页。
     *    （不能用第一个可见项：条漫一屏常同时露出多页，顶部项会比用户正在看的页靠前，
     *    导致保存的进度落后于实际阅读位置。）
     */
    private fun calculateCurrentPageIndex(): Int {
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo

        if (visibleItems.isEmpty() || layoutInfo.totalItemsCount == 0) return 0

        val lastVisibleItem = visibleItems.last()

        // 判定是否到底：最后一项可见且底部在视口内
        val isLastItemVisible = lastVisibleItem.index == layoutInfo.totalItemsCount - 1
        if (isLastItemVisible) {
            val isBottomEdgeVisible =
                (lastVisibleItem.offset + lastVisibleItem.size) <= layoutInfo.viewportEndOffset
            if (isBottomEdgeVisible) {
                return lastVisibleItem.index
            }
        }

        // 视口中心线位置（与 item.offset 同一坐标系）
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val centerItem = visibleItems.firstOrNull { item ->
            item.offset <= viewportCenter && (item.offset + item.size) > viewportCenter
        }
        return centerItem?.index ?: lastVisibleItem.index
    }
}