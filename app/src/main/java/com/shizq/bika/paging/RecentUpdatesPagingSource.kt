package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.model.ComicSimple
import com.shizq.bika.core.model.Sort
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.util.injectLocalStatus
import javax.inject.Inject

class RecentUpdatesPagingSource @Inject constructor(
    private val api: BikaDataSource,
    private val historyDao: ReadingHistoryDao
) : PagingSource<Int, ComicSimple>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ComicSimple> {
        val page = params.key ?: 1

        return try {
            val response = api.searchComics(
                sort = Sort.NEWEST,
                page = page
            )

            val comicsPage = response.comics
            val injectedComics = comicsPage.docs.injectLocalStatus(historyDao)

            LoadResult.Page(
                data = injectedComics,
                prevKey = null,
                nextKey = if (page >= comicsPage.pages) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ComicSimple>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}