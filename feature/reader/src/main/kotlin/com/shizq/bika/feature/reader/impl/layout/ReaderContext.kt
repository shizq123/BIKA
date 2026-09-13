package com.shizq.bika.feature.reader.impl.layout

import android.content.res.Configuration
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
 * 滚动能力查询走 [ReaderController.continuousScroller] 是否为 null。
 */
@Stable
class ReaderContext(
    val layout: ReaderLayoutStrategy,
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

/** 视口宽高比达到此值即视为「宽视口」，AUTO 模式据此启用跨页。 */
internal const val WideViewportAspectRatio = 1.25f

/**
 * 视口是否够宽以容纳两页。
 *
 * 首帧 containerSize 可能还是 0×0，此时不能让 `0f / 0f`（= NaN）参与比较：
 * NaN 的任何比较都是 false，会被误判成窄视口。尺寸未知时一律返回 false，
 * 等真实尺寸到达后重算即可（调用方需把尺寸作为 remember key）。
 */
internal fun isWideViewport(widthPx: Int, heightPx: Int, isLandscape: Boolean): Boolean {
    if (isLandscape) return true
    if (widthPx <= 0 || heightPx <= 0) return false
    return widthPx.toFloat() / heightPx.toFloat() >= WideViewportAspectRatio
}

/**
 * 跨页（一屏两页）的最终判定。
 *
 * 只有翻页类 viewer 支持跨页：条漫是连续滚动，没有「一屏」的概念。
 */
internal fun resolveDoublePage(
    viewerType: ViewerType,
    bookSpreadsMode: BookSpreadsMode,
    isWideViewport: Boolean,
): Boolean {
    if (viewerType != ViewerType.Pager) return false
    return when (bookSpreadsMode) {
        BookSpreadsMode.SINGLE -> false
        BookSpreadsMode.DOUBLE -> true
        BookSpreadsMode.AUTO -> isWideViewport
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
    val containerSize = LocalWindowInfo.current.containerSize
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 只用「宽/窄」这个布尔结果做 key，不用整个 containerSize：
    // 分屏拖拽、折叠屏展开过程中 containerSize 会连续变化，若整个尺寸都进 key，
    // 每一帧尺寸变化都会重建 PageSpreadState（下面的 spreadState），丢掉已经
    // 测出的宽页信息。只有跨越宽/窄阈值那一刻才需要重算。
    val isWideViewport = isWideViewport(containerSize.width, containerSize.height, isLandscape)
    val useDoublePage = remember(
        isWideViewport,
        config.bookSpreadsMode,
        readingMode.viewerType,
    ) {
        resolveDoublePage(
            viewerType = readingMode.viewerType,
            bookSpreadsMode = config.bookSpreadsMode,
            isWideViewport = isWideViewport,
        )
    }

    return when (readingMode.viewerType) {
        ViewerType.Scrolling -> {
            key(chapterOrder) {
                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = initialPageIndex
                )

                val layout = remember(listState, readingMode.hasPageGap, config.magnifierEnabled) {
                    WebtoonLayoutStrategy(
                        listState = listState,
                        hasPageGap = readingMode.hasPageGap,
                        magnifierEnabled = config.magnifierEnabled,
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
                    remember(
                        pagerState,
                        readingMode.direction,
                        readingMode.isRtl,
                        spreadState,
                        config.magnifierEnabled,
                    ) {
                        PagerLayoutStrategy(
                            pagerState = pagerState,
                            direction = readingMode.direction,
                            isRtl = readingMode.isRtl,
                            spreadState = spreadState,
                            magnifierEnabled = config.magnifierEnabled,
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
