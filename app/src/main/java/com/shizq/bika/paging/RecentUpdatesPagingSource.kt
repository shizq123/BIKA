package com.shizq.bika.paging

import kotlinx.coroutines.CancellationException
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.network.BikaDataSource
import javax.inject.Inject

class RecentUpdatesPagingSource @Inject constructor(
    private val api: BikaDataSource
) : PagingSource<Int, ComicSummary>() {

    var onPageInfoLoaded: ((totalPages: Int, totalCount: Int) -> Unit)? = null

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
                nextKey = if (page >= comicsPage.pages) null else page + 1
            ).also {
                onPageInfoLoaded?.invoke(comicsPage.pages, comicsPage.total)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ComicSummary>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}