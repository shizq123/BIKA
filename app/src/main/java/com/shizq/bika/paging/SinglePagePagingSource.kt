package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlin.coroutines.cancellation.CancellationException

/**
 * 一次性取回全部数据的分页源：只有一页，没有 prevKey/nextKey。
 *
 * 实现 [PageInfoReporting] 并固定上报 1 页，是为了让调用方对「所有分页源」用同一套
 * 装配逻辑，不必为不上报总页数的源保留 `else` 特例分支——那种分支一旦被漏写或误写，
 * 总页数会静默退化成 1，而不会有任何编译期或运行期提示。
 */
class SinglePagePagingSource<Key : Any, V : Any>(
    private val fetcher: suspend () -> List<V>
) : PagingSource<Key, V>(), PageInfoReporting {
    override var onPageInfoLoaded: ((totalPages: Int, totalCount: Int) -> Unit)? = null

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, V> {
        return try {
            val data = fetcher()
            onPageInfoLoaded?.invoke(1, data.size)

            // prevKey/nextKey 都是 null，Key 没有任何可推断的来源，会塌成 Nothing，
            // 因此两个分支都显式写出类型参数。
            LoadResult.Page<Key, V>(
                data = data,
                prevKey = null,
                nextKey = null
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error<Key, V>(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Key, V>): Key? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
                ?: state.closestPageToPosition(anchorPosition)?.nextKey
        }
    }
}
