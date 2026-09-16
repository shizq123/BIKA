package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.model.reader.Direction
import kotlinx.coroutines.flow.distinctUntilChanged

class PagerLayoutStrategy(
    private val pagerState: PagerState,
    private val direction: Direction,
    private val isRtl: Boolean,
    private val spreadState: PageSpreadState,
    private val magnifierEnabled: Boolean,
) : ReaderLayoutStrategy {

    @Composable
    override fun RenderContent(
        pageItems: LazyPagingItems<ChapterPage>,
        modifier: Modifier,
        onPageTap: (PageTapContext) -> Unit,
    ) {
        // 分页续拉让 itemCount 增长时推进分组。itemCount 是快照状态，
        // 读它即建立订阅，增长会触发重组再走到这里。
        spreadState.syncPageCount()

        val layout = spreadState.layout
        val spreads = layout.spreads

        // 分组重排后把视口拉回同一张真实页。
        //
        // 与之前的关键差别：请求只在**确认到位后**清除。之前是先 consume 再判
        // `target < pagerState.pageCount`，守卫为假时 anchor 已丢、滚动没做，
        // 重定位永久失效——而 pageCount 来自 spreadCount 的 lambda，与 spreads
        // 的重组之间有一帧窗口，那一帧恰好是最需要重定位的时刻。
        val relocateTo = spreadState.relocateTo
        LaunchedEffect(relocateTo, spreads) {
            val anchor = relocateTo ?: return@LaunchedEffect
            // 分组还没建立：不清除请求，等下一次重组再试。
            if (spreads.isEmpty() || pagerState.pageCount == 0) return@LaunchedEffect

            val target = spreads.spreadIndexOfPage(anchor)
            if (target >= pagerState.pageCount) return@LaunchedEffect

            if (pagerState.currentPage != target) {
                pagerState.scrollToPage(target)
            }
            // 到位才清。没到位就留着，下一帧重来。
            if (pagerState.currentPage == target) {
                spreadState.clearRelocation()
            }
        }

        val pageContent: @Composable (Int) -> Unit = { spreadIndex ->
            // spreads 与 pagerState.pageCount 都来自同一份分组，但 Pager 的
            // pageCount 更新与重组之间存在一帧的窗口，越界时退出而不是崩溃。
            val spread = spreads.getOrNull(spreadIndex)
            if (spread != null) {
                SpreadContent(pageItems, spread, onPageTap)
            }
        }

        if (direction == Direction.Vertical) {
            VerticalPager(
                state = pagerState,
                modifier = modifier,
                key = spreadKey(pageItems, spreads),
                pageContent = { pageContent(it) },
            )
        } else {
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                HorizontalPager(
                    state = pagerState,
                    modifier = modifier,
                    key = spreadKey(pageItems, spreads),
                    pageContent = { pageContent(it) },
                )
            }
        }
    }

    /**
     * 部分镜像站会返回重复的 imageId，直接用 id 作 key 会触发
     * "Key was already used" 崩溃，这里用页码+id 组合保证唯一性。
     * 双页模式下也必须提供 key：缺 key 时分组变化（宽页被测出）会让 Pager
     * 按位置复用错误的页面。
     */
    private fun spreadKey(
        pageItems: LazyPagingItems<ChapterPage>,
        spreads: List<PageSpread>,
    ): (Int) -> Any = { spreadIndex ->
        val spread = spreads.getOrNull(spreadIndex)
        if (spread == null) {
            "placeholder_$spreadIndex"
        } else {
            val start = spread.startIndex
            val id = pageItems.peek(start)?.id
            when (spread) {
                is PageSpread.Single -> if (id != null) "s_${start}_$id" else "s_placeholder_$start"
                is PageSpread.Double -> {
                    val secondId = pageItems.peek(spread.secondIndex)?.id
                    "d_${start}_${id ?: "p"}_${spread.secondIndex}_${secondId ?: "p"}"
                }
            }
        }
    }

    @Composable
    private fun SpreadContent(
        pages: LazyPagingItems<ChapterPage>,
        spread: PageSpread,
        onPageTap: (PageTapContext) -> Unit,
    ) {
        when (spread) {
            is PageSpread.Single -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                SinglePage(pages, spread.startIndex, onPageTap)
            }

            is PageSpread.Double -> Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // RTL 下 LocalLayoutDirection 已经翻转了 Row 的排列方向，
                // 这里按阅读顺序放入即可，不需要手动交换左右。
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SinglePage(pages, spread.startIndex, onPageTap)
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SinglePage(pages, spread.secondIndex, onPageTap)
                }
            }
        }
    }

    @Composable
    private fun SinglePage(
        pages: LazyPagingItems<ChapterPage>,
        index: Int,
        onPageTap: (PageTapContext) -> Unit,
    ) {
        val page = if (index < pages.itemCount) pages[index] else null
        if (page == null) {
            ChapterPageLoadStateItem(pages, index)
            return
        }
        ComicPageItem(
            page = page,
            index = index,
            zoomable = true,
            magnifierEnabled = magnifierEnabled,
            onTap = onPageTap,
            onSizeLoaded = { width, height ->
                // anchorPage 取 pagerState.settledPage 换算出的真实页码。
                //
                // 不能像之前那样在这里读 `spreadState.spreads` 再查 currentPage：
                // onSizeLoaded 来自 LaunchedEffect(intrinsicSize)，是异步的，同一帧
                // 可能有多页同时上报。第二个回调读到的分组已经被第一个改过，
                // 于是算出一个已经漂移的 anchor，还会覆盖掉先到的那个正确值。
                //
                // 用 settledPage 而非 currentPage：currentPage 在滑动过半时就会跳变，
                // 用户正在滑动中途上报会把 anchor 记成尚未落定的那一屏。
                spreadState.onPageMeasured(
                    pageIndex = index,
                    width = width,
                    height = height,
                    anchorPage = spreadState.layout
                        .positionAt(pagerState.settledPage)
                        ?.first,
                )
            },
        )
    }
}

class PagerController(
    private val pagerState: PagerState,
    private val spreadState: PageSpreadState,
) : ReaderController {

    /**
     * Pager 按页吸附，无法平滑推进偏移量，不具备连续滚动能力。
     * 调用方（自动滚动）据此判断入口是否展示，不存在"点了没反应"的静默失效。
     */
    override val continuousScroller: ContinuousScroller? = null

    /**
     * 当前屏覆盖的真实页码范围。
     *
     * 跨页模式下 first != last，这是「读到第几页」（取 first）与「读完没」
     * （取 last）能分开回答的前提。之前这里只发 startIndex，末屏是
     * Double(n-2, n-1) 时永远追不到 totalPages - 1：章节自动衔接不触发、
     * 「已读完」标记拿不到、页码徽章停在倒数第二页。
     *
     * 分组尚未建立时保留上一个已知位置，而不是回落到 0——0 会被
     * ReadingProgressManager 当成「用户在第 1 页」写进数据库，覆盖真实进度。
     */
    override var position: ReadingPositionSnapshot by mutableStateOf(
        spreadState.layout.positionAt(pagerState.currentPage)
            ?: ReadingPositionSnapshot.single(0),
    )
        private set

    override suspend fun track() {
        snapshotFlow { spreadState.layout.positionAt(pagerState.currentPage) }
            .distinctUntilChanged()
            .collect { snapshot ->
                if (snapshot != null) position = snapshot
            }
    }

    /**
     * 前进一个**翻页单位**。跨页模式下即前进两页，这是正确语义：
     * 逐页前进会让画面从 1|2 变成 2|3。
     */
    override suspend fun scrollNextPage() {
        val target = pagerState.currentPage + 1
        if (target < pagerState.pageCount) {
            pagerState.animateScrollToPage(target)
        }
    }

    override suspend fun scrollPrevPage() {
        val target = pagerState.currentPage - 1
        if (target >= 0) {
            pagerState.animateScrollToPage(target)
        }
    }

    override suspend fun scrollToPage(index: Int) {
        val spreads = spreadState.spreads
        if (spreads.isEmpty() || pagerState.pageCount == 0) return
        val target = spreads.spreadIndexOfPage(index)
        pagerState.scrollToPage(target.coerceIn(0, pagerState.pageCount - 1))
    }
}
