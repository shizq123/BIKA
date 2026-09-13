package com.shizq.bika.feature.reader.impl.layout

/**
 * 单个可见项的位置快照，只保留判定规则需要的三个字段。
 * 从 LazyListItemInfo 里拿出来是为了让判定规则脱离 Compose 运行时，可以单测。
 */
data class VisibleItemSnapshot(
    val index: Int,
    val offset: Int,
    val size: Int,
)

/**
 * [resolveListReadingPosition] 所需的最小布局快照。
 *
 * 只保留首、末两个可见项：规则只用到这两者，没必要把整个 visibleItemsInfo
 * 映射成新列表——那会在每次滚动触发的 snapshotFlow 里做一次多余分配。
 */
data class ListReadingLayoutInfo(
    val firstVisibleItem: VisibleItemSnapshot?,
    val lastVisibleItem: VisibleItemSnapshot?,
    val totalItemsCount: Int,
    val viewportEndOffset: Int,
)

/**
 * 条漫模式「当前阅读到的页码」判定规则，从 [WebtoonController] 中抽出。
 *
 * 规则：
 * 1. 视口为空或数据还没到（totalItemsCount == 0）时，沿用上一次的有效值
 *    [lastValidIndex]——不能返回 0，那会被下游当成「用户在第一页」写入进度。
 * 2. 滚动到底部，且最后一项完全可见（底边在视口内），强制视为最后一页，
 *    解决最后一页较短时无法触发已读的问题。
 * 3. 否则取第一个已经进入视口的 item（firstVisibleItemIndex），代表用户
 *    当前正在阅读的起始页。不用视口中心线：条漫图片可能远超屏幕高度数倍，
 *    中心线会滞后于用户已经看到的内容，导致进度保存落后于实际阅读位置。
 */
fun resolveListReadingPosition(
    layoutInfo: ListReadingLayoutInfo,
    lastValidIndex: Int,
): Int {
    val first = layoutInfo.firstVisibleItem
    val last = layoutInfo.lastVisibleItem
    if (first == null || last == null || layoutInfo.totalItemsCount == 0) return lastValidIndex

    val isLastItemVisible = last.index == layoutInfo.totalItemsCount - 1
    if (isLastItemVisible) {
        val isBottomEdgeVisible = (last.offset + last.size) <= layoutInfo.viewportEndOffset
        if (isBottomEdgeVisible) return last.index
    }

    return first.index
}
