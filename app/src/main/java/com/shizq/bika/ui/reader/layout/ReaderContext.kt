package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.core.model.ScreenOrientation
import com.shizq.bika.core.model.TapZoneLayout
import com.shizq.bika.core.model.ViewerType
import com.shizq.bika.paging.ChapterPage

@Stable
data class ReaderContext(
    val layout: ReaderLayout,
    val controller: ReaderController,
    val config: ReaderConfig = ReaderConfig.Default
)

data class ReaderConfig(
    val volumeKeyNavigation: Boolean,
    val readingMode: ReadingMode,
    val screenOrientation: ScreenOrientation,
    val tapZoneLayout: TapZoneLayout,
) {
    companion object {
        val Default = ReaderConfig(
            volumeKeyNavigation = false,
            readingMode = ReadingMode.WEBTOON,
            screenOrientation = ScreenOrientation.Portrait,
            tapZoneLayout = TapZoneLayout.LShape,
        )
    }
}

@Composable
fun rememberReaderContext(
    readingMode: ReadingMode,
    chapterPages: LazyPagingItems<ChapterPage>,
    config: ReaderConfig = ReaderConfig.Default,
    currentPageIndex: Int
): ReaderContext {
    return remember(readingMode, config, chapterPages) {
        when (readingMode.viewerType) {
            ViewerType.Scrolling -> {
                val listState = LazyListState(currentPageIndex)
                val controller =
                    WebtoonController(listState, chapterPages.itemCount)
                val strategy = WebtoonLayout(
                    listState = listState,
                    hasPageGap = readingMode.hasPageGap
                )
                ReaderContext(strategy, controller, config)
            }

            ViewerType.Pager -> {
                val pagerState = PagerState(currentPageIndex) { chapterPages.itemCount }
                val controller = PagerController(pagerState, chapterPages.itemCount)
                val strategy = PagerLayout(
                    pagerState = pagerState,
                    direction = readingMode.direction,
                    isRtl = readingMode.isRtl
                )
                ReaderContext(strategy, controller, config)
            }
        }
    }
}