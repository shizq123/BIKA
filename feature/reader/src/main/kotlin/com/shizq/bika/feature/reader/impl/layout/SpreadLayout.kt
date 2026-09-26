package com.shizq.bika.feature.reader.impl.layout

/**
 * 一份**完整且自洽**的跨页分组快照：页数、已测出的宽页、以及由二者推出的分组。
 *
 * 与之前分散在 [PageSpreadState] 里的可变字段相比，这里的关键区别是
 * 「分组」与「重定位请求」由同一个纯函数一次算出（见 [withMeasurement]），
 * 因此二者不可能不一致。之前它们分属两个字段，由渲染层的 LaunchedEffect
 * 和一个破坏性 consume 协调，那段协调逻辑无法在没有 Compose 运行时的情况下测试——
 * 而模块里最容易错的地方恰好全都在那里。
 */
data class SpreadLayout(
    val pageCount: Int,
    val doublePage: Boolean,
    val widePages: Set<Int>,
    val spreads: List<PageSpread>,
    val generation: Long,
) {
    val spreadCount: Int get() = spreads.size

    /** 翻页单位下标 → 该屏覆盖的真实页码范围；越界返回 null。 */
    fun positionAt(spreadIndex: Int): ReadingPositionSnapshot? =
        when (val spread = spreads.getOrNull(spreadIndex)) {
            null -> null
            is PageSpread.Single -> ReadingPositionSnapshot.single(spread.startIndex)
            is PageSpread.Double -> ReadingPositionSnapshot(spread.startIndex, spread.secondIndex)
        }

    companion object {
        fun of(pageCount: Int, doublePage: Boolean, widePages: Set<Int> = emptySet()) =
            SpreadLayout(
                pageCount = pageCount,
                doublePage = doublePage,
                widePages = widePages,
                spreads = buildPageSpreads(pageCount, doublePage, widePages),
                generation = 0L,
            )
    }
}

/**
 * 一次分组重算的结果。
 *
 * @property layout 重算后的分组。无变化时与输入是同一个实例（引用相等），
 *   调用方可据此跳过状态写入。
 * @property relocateTo 需要重新定位到的**真实页码**，null 表示当前屏未受影响。
 */
data class RegroupResult(
    val layout: SpreadLayout,
    val relocateTo: Int?,
)

/**
 * 页数变化（分页续拉）。
 *
 * 只在末尾追加分组，不会改变已有分组的边界，因此永不产生重定位请求。
 */
fun SpreadLayout.withPageCount(newPageCount: Int): SpreadLayout =
    if (newPageCount == pageCount) this
    else copy(
        pageCount = newPageCount,
        spreads = buildPageSpreads(newPageCount, doublePage, widePages),
        generation = generation + 1,
    )

/**
 * 上报一页的实测尺寸，重算分组并判断当前屏是否需要重定位。
 *
 * 重定位判据是「重排前后 [anchorPage] 所在的翻页单位下标是否真的变了」，
 * 而不是之前的「宽页是否位于 anchorPage 之前或就是它」。后者是前者的近似：
 * 位置关系只是单位下标变化的**常见成因**，不等于它本身。用精确判据的好处是
 * 不会请求任何一次实际上不需要的滚动——每次多余的重定位都是用户眼里的画面跳动。
 *
 * 一个可见的行为差异：anchorPage 自身被测出是宽页时，若它前面没有别的宽页，
 * 它所在的单位下标不变（例：D(0,1) S(2) D(3,4) 中第 2 页本就独占），此时不再
 * 产生重定位请求。旧判据会记下 anchor，随后渲染层发现 `target == currentPage`
 * 又跳过滚动，净效果相同，只是多绕一圈。
 *
 * @param anchorPage 上报时用户所在的真实页码。由调用方从**不随本次重排变化**的
 *   来源取得（见 `PagerLayoutStrategy` 中 `position.first`），不能在回调内部
 *   重新查分组——那样读到的可能已经是被上一次上报改过的分组。
 */
fun SpreadLayout.withMeasurement(
    pageIndex: Int,
    width: Float,
    height: Float,
    anchorPage: Int?,
): RegroupResult {
    // 单页模式下分组恒等于页码，宽页不影响任何换算。
    if (!doublePage) return RegroupResult(this, null)
    if (pageIndex !in 0..<pageCount) return RegroupResult(this, null)
    if (!isWidePage(width, height)) return RegroupResult(this, null)
    // 幂等：onSizeLoaded 会随重组反复触发，重复上报不得再次请求重定位。
    if (pageIndex in widePages) return RegroupResult(this, null)

    val next = copy(
        spreads = buildPageSpreads(
            pageCount, doublePage = true, widePageIndices = widePages + pageIndex
        ),
        widePages = widePages + pageIndex,
        generation = generation + 1,
    )

    if (anchorPage == null) return RegroupResult(next, null)

    val before = spreads.spreadIndexOfPage(anchorPage)
    val after = next.spreads.spreadIndexOfPage(anchorPage)
    return RegroupResult(next, if (before != after) anchorPage else null)
}
