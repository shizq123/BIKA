package com.shizq.bika.feature.reader.impl.layout

/**
 * 一个「翻页单位」：Pager 每滑动一次前进一个 [PageSpread]，而不是一页。
 *
 * 引入这层概念是为了让宽页判定只发生一次。之前的做法是让 Pager 的
 * pageCount 固定为 `(总页数 + 1) / 2`，渲染时再用 `index * 2` 反推左右页，
 * 一旦某页被判定为宽页需要独占一屏，后面的页码就会整体错位——宽页所在
 * 槽位只渲染了一页，但下一个槽位仍从 `(index + 1) * 2` 开始，中间那页被
 * 直接跳过。分组先算完再渲染就不会有这个问题。
 */
sealed interface PageSpread {
    /** 该单位中第一页在章节内的页码（0-based），进度保存与跳转都以它为准。 */
    val startIndex: Int

    data class Single(override val startIndex: Int) : PageSpread

    data class Double(
        override val startIndex: Int,
        val secondIndex: Int,
    ) : PageSpread
}

/** 该翻页单位是否包含指定页码。 */
fun PageSpread.containsPage(pageIndex: Int): Boolean = when (this) {
    is PageSpread.Single -> startIndex == pageIndex
    is PageSpread.Double -> startIndex == pageIndex || secondIndex == pageIndex
}

/**
 * 宽高比超过该值即视为跨页大图，需要独占一屏。
 * 取 1.1 而不是 1.0 是为了容忍轻微超宽的普通页。
 */
const val WIDE_PAGE_ASPECT_RATIO: Float = 1.1f

/** 宽页判定。抽成函数是因为原来这段逻辑在渲染层重复了三遍。 */
fun isWidePage(width: Float, height: Float): Boolean =
    width > 0f && height > 0f && width / height > WIDE_PAGE_ASPECT_RATIO

/**
 * 把页码序列分组成翻页单位。
 *
 * 规则：
 * - [doublePage] 为 false 时每页独立成组。
 * - 宽页独占一组。
 * - 相邻两页都不是宽页才能配对；末页落单则独立成组。
 *
 * [widePageIndices] 只包含**已经加载并测量过**的宽页。尚未加载的页按普通页
 * 参与配对，等图片实际尺寸回来后再重新分组。这意味着首次滚过跨页大图时
 * 分组会调整一次，属于预期行为——图片尺寸只能在解码后得知。
 */
fun buildPageSpreads(
    pageCount: Int,
    doublePage: Boolean,
    widePageIndices: Set<Int>,
): List<PageSpread> {
    if (pageCount <= 0) return emptyList()
    if (!doublePage) return List(pageCount) { PageSpread.Single(it) }

    val spreads = ArrayList<PageSpread>(pageCount)
    var index = 0
    while (index < pageCount) {
        val next = index + 1
        val canPair = index !in widePageIndices &&
            next < pageCount &&
            next !in widePageIndices
        if (canPair) {
            spreads += PageSpread.Double(startIndex = index, secondIndex = next)
            index += 2
        } else {
            spreads += PageSpread.Single(startIndex = index)
            index += 1
        }
    }
    return spreads
}

/**
 * 页码 -> 翻页单位下标。找不到时退到最后一组（页码越界）或第一组（负数）。
 */
fun List<PageSpread>.spreadIndexOfPage(pageIndex: Int): Int {
    if (isEmpty()) return 0
    val target = pageIndex.coerceAtLeast(0)
    val found = indexOfFirst { it.containsPage(target) }
    return if (found >= 0) found else lastIndex
}
