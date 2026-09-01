package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf

/**
 * 跨页分组的共享状态，由 [PagerLayout]（渲染）和 [PagerController]（页码换算）
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

    /** 章节总页数（真实页数，不是翻页单位数）。 */
    val pageCount: Int get() = pageCountProvider()

    val spreads: List<PageSpread> get() = spreadsState.value

    /** Pager 的 pageCount，即翻页单位数量。 */
    val spreadCount: Int get() = spreads.size

    /**
     * 图片解码后上报实际尺寸。只在判定为宽页时写入，避免无谓的重组。
     */
    fun onPageMeasured(pageIndex: Int, width: Float, height: Float) {
        if (isWidePage(width, height) && widePages[pageIndex] != true) {
            widePages[pageIndex] = true
        }
    }
}
