package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.core.model.ViewerType
import com.shizq.bika.paging.ComicPage

@Stable
data class ReaderContext(
    val layout: ReaderLayout,
    val controller: ReaderController
)

@Composable
fun rememberReaderContext(
    readingMode: ReadingMode,
    comicPages: LazyPagingItems<ComicPage>,
    currentPageIndex: Int
): ReaderContext {
    return remember(readingMode) {
        when (readingMode.viewerType) {
            ViewerType.Scrolling -> {
                val listState = LazyListState(currentPageIndex)
                val controller =
                    WebtoonController(listState, comicPages.itemCount)
                val strategy = WebtoonLayout(
                    listState = listState,
                    hasPageGap = readingMode.hasPageGap
                )
                ReaderContext(strategy, controller)
            }

            ViewerType.Pager -> {
                val pagerState = PagerState(currentPageIndex) { comicPages.itemCount }
                val controller = PagerController(pagerState, comicPages.itemCount)
                val strategy = PagerLayout(
                    pagerState = pagerState,
                    direction = readingMode.direction,
                    isRtl = readingMode.isRtl
                )
                ReaderContext(strategy, controller)
            }
        }
    }
}