package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.paging.ComicPage

class WebtoonLayout(
    private val listState: LazyListState,
    private val hasPageGap: Boolean
) : ReaderLayout {
    @Composable
    override fun Content(
        comicPages: LazyPagingItems<ComicPage>,
        modifier: Modifier,
        onCurrentPageChanged: (Int) -> Unit,
    ) {
        val currentPageIndex by remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                val totalItemsCount = layoutInfo.totalItemsCount

                // 如果列表为空或还没有布局信息，返回 0
                if (visibleItemsInfo.isEmpty() || totalItemsCount == 0) {
                    0
                } else {
                    // 判断是否滚动到了最底部
                    val lastVisibleItem = visibleItemsInfo.last()

                    // 如果最后一个可见项的索引等于总项数的最后一个索引...
                    if (lastVisibleItem.index == totalItemsCount - 1) {
                        // ...并且该项的底部已经完全进入视口，那么我们就认为到达了底部。
                        // 这是一个更精确的检查，防止在最后一项刚露头时就跳转。
                        val isBottomVisible =
                            lastVisibleItem.offset + lastVisibleItem.size <= layoutInfo.viewportEndOffset
                        if (isBottomVisible) {
                            // 强制返回最后一项的索引
                            totalItemsCount - 1
                        } else {
                            // 如果最后一项还没完全进入，仍然以上面的为准
                            listState.firstVisibleItemIndex
                        }
                    } else {
                        // 3. 在其他所有情况下，使用第一个可见项的索引
                        listState.firstVisibleItemIndex
                    }
                }
            }
        }
        val currentOnCurrentPageChanged by rememberUpdatedState(onCurrentPageChanged)
        LaunchedEffect(listState) {
            snapshotFlow { currentPageIndex }
                .collect { index ->
                    currentOnCurrentPageChanged(index)
                }
        }
        LazyColumn(
            state = listState,
            modifier = modifier,
            verticalArrangement = if (hasPageGap) Arrangement.spacedBy(8.dp) else Arrangement.Top
        ) {
            items(
                count = comicPages.itemCount,
                key = comicPages.itemKey { it.id },
            ) { index ->
                comicPages[index]?.let { ComicPageItem(it, index) }
            }
        }
    }
}

class WebtoonController(
    private val listState: LazyListState,
    override val totalPages: Int
) : ReaderController {
    override val visibleItemIndex: Int get() = listState.firstVisibleItemIndex

    override suspend fun nextPage(value: Float) {
        listState.animateScrollBy(value)
    }

    override suspend fun prevPage(value: Float) {
        listState.animateScrollBy(-value)
    }

    override suspend fun scrollToPage(index: Int) {
        listState.scrollToItem(index)
    }
}