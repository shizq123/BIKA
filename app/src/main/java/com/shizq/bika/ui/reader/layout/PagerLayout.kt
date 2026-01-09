package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.core.model.Direction
import com.shizq.bika.paging.ChapterPage
import kotlinx.coroutines.flow.distinctUntilChanged

class PagerLayout(
    private val pagerState: PagerState,
    private val direction: Direction,
    private val isRtl: Boolean
) : ReaderLayout {

    @Composable
    override fun Content(
        chapterPages: LazyPagingItems<ChapterPage>,
        modifier: Modifier,
    ) {
        val pageContent: @Composable (Int) -> Unit = { pageIndex ->
            PageItem(chapterPages, pageIndex)
        }

        if (direction == Direction.Vertical) {
            VerticalPager(
                state = pagerState,
                modifier = modifier,
                key = chapterPages.itemKey { it.id },
                pageContent = { pageContent(it) }
            )
        } else {
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                HorizontalPager(
                    state = pagerState,
                    modifier = modifier,
                    key = chapterPages.itemKey { it.id },
                    pageContent = { pageContent(it) }
                )
            }
        }
    }

    @Composable
    private fun PageItem(pages: LazyPagingItems<ChapterPage>, index: Int) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            pages[index]?.let { Image(it.url, index) }
        }
    }
}

class PagerController(
    private val pagerState: PagerState,
) : ReaderController {
    override val totalPages: Int
        get() = pagerState.pageCount
    override val visibleItemIndex = snapshotFlow {
        pagerState.currentPage
    }.distinctUntilChanged()

    override suspend fun scrollNextPage() {
        pagerState.animateScrollToPage(pagerState.currentPage + 1)
    }

    override suspend fun scrollPrevPage() {
        pagerState.animateScrollToPage(pagerState.currentPage - 1)
    }

    override suspend fun scrollToPage(index: Int) {
        pagerState.scrollToPage(index)
    }
}