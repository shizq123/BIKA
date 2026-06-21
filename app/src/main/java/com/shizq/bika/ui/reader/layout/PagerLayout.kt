package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.core.model.Direction
import com.shizq.bika.paging.ChapterPage
import kotlinx.coroutines.flow.distinctUntilChanged

class PagerLayout(
    private val pagerState: PagerState,
    private val direction: Direction,
    private val isRtl: Boolean,
    private val useDoublePage: Boolean
) : ReaderPageLayout {

    @Composable
    override fun Content(
        pageItems: LazyPagingItems<ChapterPage>,
        modifier: Modifier,
    ) {
        val widePages = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
        val pageContent: @Composable (Int) -> Unit = { pageIndex ->
            if (useDoublePage) {
                DoublePageItem(pageItems, pageIndex, widePages)
            } else {
                PageItem(pageItems, pageIndex)
            }
        }

        if (direction == Direction.Vertical) {
            VerticalPager(
                state = pagerState,
                modifier = modifier,
                key = if (useDoublePage) null else pageItems.itemKey { it.id },
                pageContent = { pageContent(it) }
            )
        } else {
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                HorizontalPager(
                    state = pagerState,
                    modifier = modifier,
                    key = if (useDoublePage) null else pageItems.itemKey { it.id },
                    pageContent = { pageContent(it) }
                )
            }
        }
    }

    @Composable
    private fun PageItem(pages: LazyPagingItems<ChapterPage>, index: Int) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            pages[index]?.let { ComicPageItem(it, index) }
        }
    }

    @Composable
    private fun DoublePageItem(
        pages: LazyPagingItems<ChapterPage>,
        index: Int,
        widePages: MutableMap<String, Boolean>
    ) {
        val leftIndex = index * 2
        val rightIndex = index * 2 + 1

        val firstPage = if (leftIndex < pages.itemCount) pages[leftIndex] else null
        val secondPage = if (rightIndex < pages.itemCount) pages[rightIndex] else null

        val isFirstPageWide = firstPage?.let {
            widePages[it.id] ?: false
        } ?: false

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isFirstPageWide || secondPage == null) {
                firstPage?.let {
                    ComicPageItem(
                        page = it,
                        index = leftIndex,
                        onSizeLoaded = { width, height ->
                            if (width > 0 && height > 0 && width / height > 1.1f) {
                                widePages[it.id] = true
                            }
                        }
                    )
                }
            } else {
                val nonNullFirst = requireNotNull(firstPage)
                val nonNullSecond = requireNotNull(secondPage)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRtl) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            ComicPageItem(nonNullSecond, rightIndex)
                        }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            ComicPageItem(
                                page = nonNullFirst,
                                index = leftIndex,
                                onSizeLoaded = { width, height ->
                                    if (width > 0 && height > 0 && width / height > 1.1f) {
                                        widePages[nonNullFirst.id] = true
                                    }
                                }
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            ComicPageItem(
                                page = nonNullFirst,
                                index = leftIndex,
                                onSizeLoaded = { width, height ->
                                    if (width > 0 && height > 0 && width / height > 1.1f) {
                                        widePages[nonNullFirst.id] = true
                                    }
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            ComicPageItem(nonNullSecond, rightIndex)
                        }
                    }
                }
            }
        }
    }
}

class PagerController(
    private val pagerState: PagerState,
    private val useDoublePage: Boolean
) : ReaderController {
    override val totalPages: Int
        get() = if (useDoublePage) pagerState.pageCount * 2 else pagerState.pageCount

    override val visibleItemIndex = snapshotFlow {
        if (useDoublePage) pagerState.currentPage * 2 else pagerState.currentPage
    }.distinctUntilChanged()

    override suspend fun scrollNextPage() {
        pagerState.animateScrollToPage(pagerState.currentPage + 1)
    }

    override suspend fun scrollPrevPage() {
        pagerState.animateScrollToPage(pagerState.currentPage - 1)
    }

    override suspend fun scrollToPage(index: Int) {
        if (pagerState.pageCount > 0) {
            val target = if (useDoublePage) index / 2 else index
            val clampedTarget = target.coerceIn(0, pagerState.pageCount - 1)
            pagerState.scrollToPage(clampedTarget)
        }
    }
}