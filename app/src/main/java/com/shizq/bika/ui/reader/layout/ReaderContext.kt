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
import com.shizq.bika.ui.reader.util.preload.LazyListScrollStateProvider
import com.shizq.bika.ui.reader.util.preload.PagerScrollStateProvider
import com.shizq.bika.ui.reader.util.preload.ScrollStateProvider

@Stable
data class ReaderContext(
    val layout: ReaderLayout,
    val controller: ReaderController,
    val scrollStateProvider: ScrollStateProvider,
    val config: ReaderConfig = ReaderConfig.Default
)

data class ReaderConfig(
    val volumeKeyNavigation: Boolean,
    val readingMode: ReadingMode,
    val screenOrientation: ScreenOrientation,
    val tapZoneLayout: TapZoneLayout,
    val preloadCount: Int,
) {
    companion object {
        val Default = ReaderConfig(
            volumeKeyNavigation = false,
            readingMode = ReadingMode.WEBTOON,
            screenOrientation = ScreenOrientation.Portrait,
            tapZoneLayout = TapZoneLayout.LShape,
            preloadCount = 0,
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

            val layout = remember(listState, readingMode.hasPageGap) {
                WebtoonLayout(
                    listState = listState,
                    hasPageGap = readingMode.hasPageGap
                )
            }
            val controller = remember(listState) { WebtoonController(listState) }
            val scrollProvider = remember(listState) { LazyListScrollStateProvider(listState) }

            ReaderContext(
                layout = layout,
                controller = controller,
                scrollStateProvider = scrollProvider,
                config = config
            )
        }

        ViewerType.Pager -> {
            val pagerState =
                rememberPagerState(initialPage = initialPageIndex) { chapterPages.itemCount }

            val layout = remember(pagerState, readingMode.direction, readingMode.isRtl) {
                PagerLayout(
                    pagerState = pagerState,
                    direction = readingMode.direction,
                    isRtl = readingMode.isRtl
                )
            }

            val controller = remember(pagerState) { PagerController(pagerState) }
            val scrollProvider = remember(pagerState) { PagerScrollStateProvider(pagerState) }

            ReaderContext(
                layout = layout,
                controller = controller,
                scrollStateProvider = scrollProvider,
                config = config
            )
        }
    }
}