package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
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
    initialPageIndex: Int
): ReaderContext {
    return when (readingMode.viewerType) {
        ViewerType.Scrolling -> {
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPageIndex)

            remember(readingMode, config, listState) {
                val controller = WebtoonController(listState)

                val layout = WebtoonLayout(
                    listState = listState,
                    hasPageGap = readingMode.hasPageGap
                )

                ReaderContext(layout, controller, config)
            }
        }

        ViewerType.Pager -> {
            val pagerState = rememberPagerState(initialPage = initialPageIndex) {
                chapterPages.itemCount
            }

            remember(readingMode, config, pagerState) {
                val controller = PagerController(pagerState)

                val layout = PagerLayout(
                    pagerState = pagerState,
                    direction = readingMode.direction,
                    isRtl = readingMode.isRtl
                )

                ReaderContext(layout, controller, config)
            }
        }
    }
}