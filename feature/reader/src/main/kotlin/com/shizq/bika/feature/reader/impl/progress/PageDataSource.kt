package com.shizq.bika.feature.reader.impl.progress

import androidx.compose.runtime.snapshotFlow
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.data.paging.ChapterPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 页面数据源抽象
 * 提供响应式的数据加载状态
 */
interface PageDataSource {
    /**
     * 获取当前已加载的页数（即时查询）
     */
    val loadedCount: Int

    /**
     * 已加载页数的响应式流
     * 当数据加载状态变化时自动发射新值
     */
    val loadedCountFlow: Flow<Int>
}

/**
 * LazyPagingItems 的数据源适配器
 * 利用 Compose 的 snapshotFlow 实现响应式监听
 */
class PagingDataSource(
    private val items: LazyPagingItems<ChapterPage>
) : PageDataSource {

    override val loadedCount: Int
        get() = items.itemCount

    override val loadedCountFlow: Flow<Int> = snapshotFlow {
        items.itemCount
    }.distinctUntilChanged()
}
