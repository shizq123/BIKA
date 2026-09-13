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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.model.reader.Direction
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class PagerLayoutStrategy(
    private val pagerState: PagerState,
    private val direction: Direction,
    private val isRtl: Boolean,
    private val spreadState: PageSpreadState,
    private val magnifierEnabled: Boolean,
) : ReaderLayoutStrategy {

    /**
     * 翻页模式由每页自己缩放：容器和页面同时注册缩放手势会互相抢事件。
     * 跨页模式下也让左右两页各自独立缩放。
     */
    override val isGestureSelfContained: Boolean = true

    @Composable
    override fun RenderContent(
        pageItems: LazyPagingItems<ChapterPage>,
        modifier: Modifier,
        onPageTap: (PageTapContext) -> Unit,
    ) {
        val spreads = spreadState.spreads

        // 分组重排后把视口拉回同一张真实页。
        // 只在 onPageMeasured 判定「宽页出现在当前位置之前」时才有待处理值，
        // 因此不会与进度恢复（ProgressManager 的 scrollToPage）互相打断：
        // 分页加载导致的 itemCount 增长只在末尾追加分组，不会触发重定位。
        val pendingAnchor = spreadState.pendingAnchorPage
        LaunchedEffect(pendingAnchor, spreads) {
            val anchor = spreadState.consumePendingAnchor() ?: return@LaunchedEffect
            val target = spreads.spreadIndexOfPage(anchor)
            if (target != pagerState.currentPage && target < pagerState.pageCount) {
                pagerState.scrollToPage(target)
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
                // anchorPage 取上报**当时**用户所在的真实页码：测出宽页会改变分组，
                // 若该宽页在当前位置之前，pagerState.currentPage 的含义会漂移一位。
                spreadState.onPageMeasured(
                    pageIndex = index,
                    width = width,
                    height = height,
                    anchorPage = spreadState.spreads
                        .getOrNull(pagerState.currentPage)
                        ?.startIndex,
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
     * 当前页码取所在翻页单位的首页。
     *
     * 分组尚未建立时（章节切换后 itemCount 仍为 0）不发射，而不是发 0。
     * 发 0 会被下游当成"用户正停在第 1 页"：ReadingProgressManager 会把它
     * 写进数据库，覆盖掉真实进度；页码徽章也会闪一下 "1 / N"。
     * 下游用 collectAsState(0) 自带初始值，等真实页码到达即可。
     */
    override val visibleItemIndex = snapshotFlow {
        spreadState.spreads.getOrNull(pagerState.currentPage)?.startIndex
    }.filterNotNull().distinctUntilChanged()

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

/** Pager 的可见页范围：跨页模式下一屏包含两页，预载需要知道真实页码。 */
internal fun PageSpreadState.visibleSpreadRange(pagerState: PagerState) = snapshotFlow {
    pagerState.currentPage
}.distinctUntilChanged().map { spreadIndex ->
    when (val spread = spreads.getOrNull(spreadIndex)) {
        null -> null
        is PageSpread.Single -> spread.startIndex..spread.startIndex
        is PageSpread.Double -> spread.startIndex..spread.secondIndex
    }
}
