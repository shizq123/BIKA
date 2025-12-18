package com.shizq.bika.ui.reader.util.preload

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
 * ScrollStateProvider 的 PagerState 实现。
 */
internal class PagerScrollStateProvider(
    private val pagerState: PagerState
) : ScrollStateProvider {
    override val visibleItemsRange: Flow<IntRange?> = snapshotFlow {
        pagerState.currentPage
    }.distinctUntilChanged().map { it..it }
}