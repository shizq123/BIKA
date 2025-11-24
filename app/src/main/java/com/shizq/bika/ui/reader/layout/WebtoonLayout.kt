package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { index ->
                    onCurrentPageChanged(index)
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