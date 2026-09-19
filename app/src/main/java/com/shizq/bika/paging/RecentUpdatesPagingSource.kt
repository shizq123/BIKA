package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.nextPageKey
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class RecentUpdatesPagingSource @Inject constructor(
    private val api: BikaDataSource
) : PagingSource<Int, ComicSummary>(), PageInfoReporting {

    override var onPageInfoLoaded: ((totalPages: Int, totalCount: Int) -> Unit)? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ComicSummary> {
        val page = params.key ?: 1

        return try {
            val response = api.searchComics(
                sort = SortOrder.NEWEST,
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
}