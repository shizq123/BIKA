package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.gestures.ScrollableDefaults
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
    comicPages: LazyPagingItems<ComicPage>
): ReaderContext {
    val listFlingBehavior = ScrollableDefaults.flingBehavior()
//    val pagerState =  rememberPagerState{comicPages.itemCount }
//    val pagerFlingBehavior = PagerDefaults.flingBehavior(state = pagerState)
    return remember(readingMode) {
        when (readingMode.viewerType) {
            ViewerType.Scrolling -> {
                val listState = LazyListState()
                val controller =
                    WebtoonController(listState, listFlingBehavior, comicPages.itemCount)
                val strategy = WebtoonLayout(
                    listState = listState,
                    flingBehavior = listFlingBehavior,
                    hasPageGap = readingMode.hasPageGap
                )
                ReaderContext(strategy, controller)
            }

            ViewerType.Pager -> {
                val pagerState = PagerState { comicPages.itemCount }
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