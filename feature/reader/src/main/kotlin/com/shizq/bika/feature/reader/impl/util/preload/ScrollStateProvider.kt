package com.shizq.bika.feature.reader.impl.util.preload

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 一个抽象接口，用于提供列表的滚动状态。
 */
interface ScrollStateProvider {
    /**
     * 一个 Flow，持续发射当前可见项的索引范围 (first..last)。
     * 如果列表为空或未布局，可以发射 null。
     */
    val visibleItemsRange: Flow<IntRange?>
}

/**
 * ScrollStateProvider 的 LazyListState 实现。
 */
internal class LazyListScrollStateProvider(
    private val listState: LazyListState
) : ScrollStateProvider {
    override val visibleItemsRange: Flow<IntRange?> = snapshotFlow {
        val layoutInfo = listState.layoutInfo
        val first = layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index
        if (first != null && last != null) first..last else null
    }.distinctUntilChanged()
}

/**
 * 翻页模式的实现：接收一个已经换算成**真实页码**范围的 Flow。
 *
 * 不直接持有 PagerState 是因为 `pagerState.currentPage` 是「翻页单位」下标而不是
 * 页码。跨页模式一屏两页，按 currentPage 预载会漏掉右页；有宽页独占一屏时
 * 单位下标与页码的偏移还会逐渐累积。换算依赖跨页分组
 * （[com.shizq.bika.feature.reader.impl.layout.PageSpreadState]），
 * 由 layout 包算好后传入，避免 preload 包反向依赖 layout 包。
 *
 * 单页模式同样走这里：分组会退化成每页一个单位，行为与直接用 currentPage 一致。
 */
internal class SpreadScrollStateProvider(
    override val visibleItemsRange: Flow<IntRange?>
) : ScrollStateProvider
