package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.nextPageKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * 「某位骑士上传的本子」列表。
 *
 * 必须按 [knightId] 查询（对应接口的 `ca` 参数），不能拿骑士昵称去做关键词搜索——
 * 昵称搜索通常也能返回结果，只是结果与该骑士无关，属于不会报错的静默错误。
 */
class KnightPagingSource @AssistedInject constructor(
    private val api: BikaDataSource,
    @Assisted private val knightId: String,
    @Assisted private val sort: SortOrder,
) : PagingSource<Int, ComicSummary>(), PageInfoReporting {
    override var onPageInfoLoaded: ((totalPages: Int, totalCount: Int) -> Unit)? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ComicSummary> {
        val page = params.key ?: 1

        return try {
            val response = api.searchComics(
                knightId = knightId,
                sort = sort,
                page = page
            )

            val comicsPage = response.comics

            LoadResult.Page<Int, ComicSummary>(
                data = comicsPage.docs,
                prevKey = null,
                nextKey = comicsPage.nextPageKey(page)
            ).also {
                onPageInfoLoaded?.invoke(comicsPage.pages, comicsPage.total)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ComicSummary>): Int? = null

    @AssistedFactory
    interface Factory {
        fun create(knightId: String, sort: SortOrder): KnightPagingSource
    }
}
