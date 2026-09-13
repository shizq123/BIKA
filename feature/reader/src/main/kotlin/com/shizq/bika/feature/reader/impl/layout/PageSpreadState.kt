package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 跨页分组的共享状态，由 [PagerLayoutStrategy]（渲染）和 [PagerController]（页码换算）
 * 共同读取，保证两边看到的是同一份分组。
 *
 * 分组结果是 derived 的：宽页集合或总页数变化时自动重算，调用方不需要手动同步。
 * 之前渲染层各算一次、控制层用 `index * 2` 另算一次，两边会不一致。
 */
@Stable
class PageSpreadState(
    private val doublePage: Boolean,
    private val pageCountProvider: () -> Int,
) {
    /** 已测量出的宽页页码。用 Map 是因为 Compose 没有 mutableStateSetOf。 */
    private val widePages = mutableStateMapOf<Int, Boolean>()

    private val spreadsState = derivedStateOf {
        buildPageSpreads(
            pageCount = pageCountProvider(),
            doublePage = doublePage,
            widePageIndices = widePages.keys,
        )
    }

    val spreads: List<PageSpread> get() = spreadsState.value

    /** Pager 的 pageCount，即翻页单位数量。 */
    val spreadCount: Int get() = spreads.size

    /**
     * 分组变化后需要重新定位到的**真实页码**，null 表示无待处理的重定位。
     * 由 [PagerLayoutStrategy] 消费，见 [consumePendingAnchor]。
     */
    var pendingAnchorPage: Int? by mutableStateOf(null)
        private set

    /**
     * 图片解码后上报实际尺寸。只在判定为宽页时写入，避免无谓的重组。
     *
     * [anchorPage] 是上报时用户所在的真实页码。测出一个位于它**之前**的宽页会
     * 让该页独占一屏、后续所有页的分组整体后移一位，此时 pagerState.currentPage
     * （翻页单位下标）含义漂移，用户会看到画面莫名跳到邻页。这里把当时的真实
     * 页码记下来，交由渲染层在分组稳定后滚回同一页。
     *
     * 只在宽页位于 anchorPage 之前（或就是它）时才记：位于之后的宽页只影响
     * 尚未看到的分组，当前屏不会变。
     */
    fun onPageMeasured(pageIndex: Int, width: Float, height: Float, anchorPage: Int? = null) {
        if (!isWidePage(width, height) || widePages[pageIndex] == true) return

        if (anchorPage != null && pageIndex <= anchorPage) {
            pendingAnchorPage = anchorPage
        }
        widePages[pageIndex] = true
    }

    /** 取出并清空待重定位页码；返回 null 表示无需重定位。 */
    fun consumePendingAnchor(): Int? = pendingAnchorPage.also { pendingAnchorPage = null }
}
