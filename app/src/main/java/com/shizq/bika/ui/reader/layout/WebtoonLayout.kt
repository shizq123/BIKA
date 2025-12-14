package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.paging.ChapterPage
import com.shizq.bika.ui.reader.util.rememberCheckerboardBrush
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class WebtoonLayout(
    private val listState: LazyListState,
    private val hasPageGap: Boolean
) : ReaderLayout {
    @Composable
    override fun Content(
        chapterPages: LazyPagingItems<ChapterPage>,
        modifier: Modifier,
    ) {
        val sharedCheckerboardBrush = rememberCheckerboardBrush()
        LazyColumn(
            state = listState,
            modifier = modifier,
            verticalArrangement = if (hasPageGap) Arrangement.spacedBy(8.dp) else Arrangement.Top
        ) {
            items(
                count = chapterPages.itemCount,
                key = chapterPages.itemKey { it.id },
            ) { index ->
                chapterPages[index]?.let {
                    Image(it.url, index, modifier = Modifier.background(sharedCheckerboardBrush))
                }
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
        listState.scrollToItem(index)
    }

    /**
     * 计算当前阅读到的页码
     * 规则：
     * 1. 优先取第一个可见项。
     * 2. 如果滚动到底部，且最后一项完全可见，强制视为最后一页（解决最后一页较短时无法触发已读的问题）。
     */
    private fun calculateCurrentPageIndex(): Int {
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo

        if (visibleItems.isEmpty() || layoutInfo.totalItemsCount == 0) return 0

        val lastVisibleItem = visibleItems.last()
        val firstVisibleItem = visibleItems.first()

        // 判定是否到底：最后一项可见且底部在视口内
        val isLastItemVisible = lastVisibleItem.index == layoutInfo.totalItemsCount - 1
        if (isLastItemVisible) {
            val isBottomEdgeVisible =
                (lastVisibleItem.offset + lastVisibleItem.size) <= layoutInfo.viewportEndOffset
            if (isBottomEdgeVisible) {
                return lastVisibleItem.index
            }
        }
        return firstVisibleItem.index
    }
}