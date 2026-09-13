package com.shizq.bika.feature.reader.impl.progress

import androidx.compose.runtime.snapshotFlow
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.data.paging.ChapterPage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/**
 * 「目标页的数据是否真的可用」这一个问题的抽象。
 *
 * 旧接口暴露的是 `loadedCount: Int` + `loadedCountFlow`，判据写成 `itemCount > targetPage`。
 * 那个判据在本项目的 Paging 配置下**恒真**：ChapterRepositoryImpl 用了
 * `enablePlaceholders = true`，且 ChapterPagesPagingSource 上报了
 * itemsBefore/itemsAfter，于是首屏一到 itemCount 就等于服务端 total，
 * 与「第 targetPage 项是否已加载」无关。
 *
 * 正确判据是 `peek(index) != null`——placeholder 位置 peek 返回 null。
 * PagerLayoutStrategy.spreadKey 里已经在用这个手法区分真实项与占位项。
 */
interface PageDataSource {
    /** 第 [index] 项是否已是真实数据（非 placeholder）。 */
    fun isLoaded(index: Int): Boolean

    /** 挂起直到第 [index] 项变为真实数据。调用方负责套超时。 */
    suspend fun awaitLoaded(index: Int)
}

/**
 * LazyPagingItems 适配器。
 *
 * [awaitLoaded] 用 snapshotFlow 观察 peek 结果：peek 读的是 Compose 快照状态，
 * 分页数据到达会触发重新求值，因此无需轮询。
 */
class PagingDataSource(
    private val items: LazyPagingItems<ChapterPage>,
) : PageDataSource {

    override fun isLoaded(index: Int): Boolean =
        index >= 0 && index < items.itemCount && items.peek(index) != null

    override suspend fun awaitLoaded(index: Int) {
        snapshotFlow { isLoaded(index) }
            .distinctUntilChanged()
            .first { it }
    }
}