package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.core.model.Direction
import com.shizq.bika.paging.ComicPage

class PagerLayout(
    private val pagerState: PagerState,
    private val direction: Direction,
    private val isRtl: Boolean
) : ReaderLayout {

    @Composable
    override fun Content(
        comicPages: LazyPagingItems<ComicPage>,
        modifier: Modifier,
        onCurrentPageChanged: (Int) -> Unit
    ) {
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }
                .collect { index ->
                    onCurrentPageChanged(index)
                }
        }

        if (direction == Direction.Vertical) {
            VerticalPager(
                state = pagerState,
                modifier = modifier,
                key = comicPages.itemKey { it.id }
            ) { idx -> PageItem(comicPages, idx) }
        } else {
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                HorizontalPager(
                    state = pagerState,
                    modifier = modifier,
                    key = comicPages.itemKey { it.id }
                ) { idx -> PageItem(comicPages, idx) }
            }
        }
    }

    @Composable
    private fun PageItem(pages: LazyPagingItems<ComicPage>, index: Int) {
        pages[index]?.let { ComicPageItem(it, index) }
    }
}

class PagerController(
    private val pagerState: PagerState,
    override val totalPages: Int
) : ReaderController {
    override val visibleItemIndex: Int get() = pagerState.currentPage

    override suspend fun scrollToPage(index: Int) {
        pagerState.scrollToPage(index)
    }

    override suspend fun scrollBy(value: Float) {
        pagerState.scrollBy(value)
    }

    override suspend fun animateScrollBy(value: Float) {
        pagerState.animateScrollBy(value)
    }

    override suspend fun animateScrollToItem(index: Int) {
        pagerState.animateScrollToPage(index)
    }

    override suspend fun performFling(initialVelocity: Float) {

    }
}