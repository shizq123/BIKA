package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.model.BookSpreadsMode
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
    val config: ReaderConfig = ReaderConfig.Default,
    val lazyListState: LazyListState? = null
)

data class ReaderConfig(
    val volumeKeyNavigation: Boolean,
    val readingMode: ReadingMode,
    val screenOrientation: ScreenOrientation,
    val tapZoneLayout: TapZoneLayout,
    val preloadCount: Int,
    val eyeCareEnabled: Boolean,
    val eyeCareDarkness: Float,
    val autoScrollEnabled: Boolean,
    val autoScrollSpeed: Int,
    val bookSpreadsMode: BookSpreadsMode,
    val magnifierEnabled: Boolean,
    val statusBarCapsuleEnabled: Boolean,
) {
    companion object {
        val Default = ReaderConfig(
            volumeKeyNavigation = false,
            readingMode = ReadingMode.WEBTOON,
            screenOrientation = ScreenOrientation.Portrait,
            tapZoneLayout = TapZoneLayout.LShape,
            preloadCount = 0,
            eyeCareEnabled = false,
            eyeCareDarkness = 0.3f,
            autoScrollEnabled = false,
            autoScrollSpeed = 3,
            bookSpreadsMode = BookSpreadsMode.AUTO,
            magnifierEnabled = true,
            statusBarCapsuleEnabled = true,
        )
    }
}

@Composable
fun rememberReaderContext(
    readingMode: ReadingMode,
    chapterPages: LazyPagingItems<ChapterPage>,
    config: ReaderConfig = ReaderConfig.Default,
    initialPageIndex: Int,
    chapterOrder: Int,
): ReaderContext {
    val configuration = LocalConfiguration.current
    val isLargeOrLandscape = remember(configuration) {
        val aspect = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
        aspect >= 1.25f || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    }
    val useDoublePage = remember(config.bookSpreadsMode, isLargeOrLandscape, readingMode) {
        if (readingMode.viewerType != ViewerType.Pager) {
            false
        } else {
            when (config.bookSpreadsMode) {
                BookSpreadsMode.SINGLE -> false
                BookSpreadsMode.DOUBLE -> true
                BookSpreadsMode.AUTO -> isLargeOrLandscape
            }
        }
    }

    return when (readingMode.viewerType) {
        ViewerType.Scrolling -> {
            // 用 key(chapterOrder) 强制在章节切换时重建 listState，
            // 确保 initialFirstVisibleItemIndex（即上次阅读位置）每次都能生效。
            key(chapterOrder) {
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
                    config = config,
                    lazyListState = listState
                )
            }
        }

        ViewerType.Pager -> {
            // 同理，chapter 切换时强制重建 pagerState。
            key(chapterOrder) {
                val pageCount = if (useDoublePage) (chapterPages.itemCount + 1) / 2 else chapterPages.itemCount
                val pagerState =
                    rememberPagerState(initialPage = if (useDoublePage) initialPageIndex / 2 else initialPageIndex) { pageCount }

                val layout = remember(pagerState, readingMode.direction, readingMode.isRtl, useDoublePage) {
                    PagerLayout(
                        pagerState = pagerState,
                        direction = readingMode.direction,
                        isRtl = readingMode.isRtl,
                        useDoublePage = useDoublePage
                    )
                }

                val controller = remember(pagerState, useDoublePage) { PagerController(pagerState, useDoublePage) }
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
}

val LocalReaderConfig = androidx.compose.runtime.staticCompositionLocalOf { ReaderConfig.Default }