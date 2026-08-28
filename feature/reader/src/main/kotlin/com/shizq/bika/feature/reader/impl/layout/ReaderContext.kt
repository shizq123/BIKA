package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.model.BookSpreadsMode
import com.shizq.bika.core.model.reader.ReadingMode
import com.shizq.bika.core.model.reader.ScreenOrientation
import com.shizq.bika.core.model.reader.TapZoneLayout
import com.shizq.bika.core.model.reader.ViewerType
import com.shizq.bika.feature.reader.impl.util.preload.LazyListScrollStateProvider
import com.shizq.bika.feature.reader.impl.util.preload.ScrollStateProvider
import com.shizq.bika.feature.reader.impl.util.preload.SpreadScrollStateProvider

/**
 * 不再暴露 LazyListState：那会绕过 [ReaderController] 的抽象，
 * 让调用方用 `lazyListState != null` 反推「是不是条漫模式」。
 * 滚动能力查询走 [ReaderController.supportsContinuousScroll]。
 */
@Stable
class ReaderContext(
    val layout: ReaderLayout,
    val controller: ReaderController,
    val scrollStateProvider: ScrollStateProvider,
    val config: ReaderConfig = ReaderConfig.Default,
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
    val windowInfo = LocalWindowInfo.current
    val isLargeOrLandscape = remember(configuration) {
        val aspect = windowInfo.containerSize.width.toFloat() / windowInfo.containerSize.height.toFloat()
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
            key(chapterOrder) {
                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = initialPageIndex
                )

                val layout = remember(listState, readingMode.hasPageGap) {
                    WebtoonLayout(
                        listState = listState,
                        hasPageGap = readingMode.hasPageGap
                    )
                }
                val controller = remember(listState) { WebtoonController(listState, initialPageIndex) }
                val scrollProvider = remember(listState) { LazyListScrollStateProvider(listState) }

                ReaderContext(
                    layout = layout,
                    controller = controller,
                    scrollStateProvider = scrollProvider,
                    config = config,
                )
            }
        }

        ViewerType.Pager -> {
            // key(chapterOrder) 与 Scrolling 分支对齐：不加的话切章时 pagerState 被复用，
            // initialPage 只在首次创建生效，新章节会停在旧页码上。
            key(chapterOrder) {
                // 分组状态先建立：pagerState 的 pageCount 要取翻页单位数，
                // 不能再用 (itemCount + 1) / 2 —— 出现宽页独占一屏时该公式会算少，
                // 页码从宽页之后开始整体错位。
                val spreadState = remember(useDoublePage) {
                    PageSpreadState(
                        doublePage = useDoublePage,
                        pageCountProvider = { chapterPages.itemCount },
                    )
                }

                val pagerState = rememberPagerState(
                    // initialPage 只在创建时读一次，此刻 chapterPages.itemCount 往往还是 0、
                    // 分组结果为空，查 spreadIndexOfPage 只会得到 0。创建时也还没有任何页被
                    // 测量过，等价于「无宽页」，此时单位下标就是 index/2，直接算即可。
                    // 真正的进度定位由 ProgressManager 调 scrollToPage 完成（那里会查分组）。
                    initialPage = if (useDoublePage) initialPageIndex / 2 else initialPageIndex
                ) { spreadState.spreadCount }

                val layout =
                    remember(pagerState, readingMode.direction, readingMode.isRtl, spreadState) {
                        PagerLayout(
                            pagerState = pagerState,
                            direction = readingMode.direction,
                            isRtl = readingMode.isRtl,
                            spreadState = spreadState,
                        )
                    }

                val controller = remember(pagerState, spreadState) {
                    PagerController(pagerState, spreadState)
                }
                // 预载要按真实页码走：跨页时一屏有两页，只报 currentPage 会漏预载右页。
                val scrollProvider = remember(pagerState, spreadState) {
                    SpreadScrollStateProvider(spreadState.visibleSpreadRange(pagerState))
                }

                ReaderContext(
                    layout = layout,
                    controller = controller,
                    scrollStateProvider = scrollProvider,
                    config = config,
                )
            }
        }
    }
}

val LocalReaderConfig = androidx.compose.runtime.staticCompositionLocalOf { ReaderConfig.Default }