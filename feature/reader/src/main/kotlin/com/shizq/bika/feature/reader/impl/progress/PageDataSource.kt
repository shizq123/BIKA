package com.shizq.bika.feature.reader.impl.progress

import androidx.compose.runtime.snapshotFlow
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.data.paging.ChapterPage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
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

    /**
     * 挂起直到第 [index] 项变为真实数据，或判定它越界。调用方仍需套超时
     * （数据可能既没到位也没被判定越界，例如一直在加载中）。
     *
     * 越界这条出口是新增的：数据库里的 pageIndex 可能大于章节现在的页数
     * （服务端删图、章节重排），此时 peek(index) 永远是 null，旧的
     * `awaitLoaded` 只能等满 dataWaitTimeout（10s）才失败，每次进这一章都卡 10 秒。
     */
    suspend fun awaitLoadedOrBounds(index: Int): PageLoadResult
}

/** [PageDataSource.awaitLoadedOrBounds] 的结果。 */
sealed interface PageLoadResult {
    /** 目标页已是真实数据，可以滚过去了。 */
    data object Loaded : PageLoadResult

    /**
     * 目标页超出章节范围，等下去也不会到位。
     *
     * @property actualTotal 判定时章节的总项数，仅用于日志/提示。
     */
    data class OutOfBounds(val actualTotal: Int) : PageLoadResult
}

/**
 * LazyPagingItems 适配器。
 *
 * [awaitLoadedOrBounds] 用 snapshotFlow 同时观察 peek 与 loadState：
 * 1. `peek(index) != null` → [PageLoadResult.Loaded]
 * 2. 首次加载已结束（refresh 非 Loading）、Paging 当前并没有在取数据
 *    （append 非 Loading）、而 index 仍在 itemCount 之外 → [PageLoadResult.OutOfBounds]
 * 3. 其余情况发 null，继续等
 *
 * 第 2 条为什么可以立刻判定：本项目 enablePlaceholders = true 且分页源上报了
 * itemsBefore/itemsAfter，首屏一到 itemCount 就等于服务端 total，所以
 * 「index >= itemCount」在首次加载完成后就是可信的越界判据。
 * 要求 append 处于 NotLoading 是为了兼容 computePlaceholderCounts 退化成
 * COUNT_UNDEFINED 的路径——那条路径下 itemCount 会随 append 增长，
 * 必须等它不再增长时才做判断。
 *
 * peek 与 loadState 都是 Compose 快照状态，数据到达会触发重新求值，无需轮询。
 */
class PagingDataSource(
    private val items: LazyPagingItems<ChapterPage>,
) : PageDataSource {

    override fun isLoaded(index: Int): Boolean =
        index >= 0 && index < items.itemCount && items.peek(index) != null

    override suspend fun awaitLoadedOrBounds(index: Int): PageLoadResult =
        snapshotFlow {
            val loadState = items.loadState
            when {
                index < 0 -> PageLoadResult.OutOfBounds(items.itemCount)

                items.peek(index) != null -> PageLoadResult.Loaded

                // 还没拿到第一页，itemCount 不可信，什么都不能断言。
                loadState.refresh is LoadState.Loading -> null

                // Paging 正在取数据，itemCount 可能还会涨，先等。
                loadState.append is LoadState.Loading -> null

                index >= items.itemCount -> PageLoadResult.OutOfBounds(items.itemCount)

                // index 在范围内但仍是 placeholder：数据还在路上（或需要滚动触发），
                // 交给调用方的超时兜底。
                else -> null
            }
        }
            .distinctUntilChanged()
            .filterNotNull()
            .first()
}
