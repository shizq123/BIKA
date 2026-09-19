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

class FavouriteComicsPagingSource @AssistedInject constructor(
    private val api: BikaDataSource,
    @Assisted private val sort: SortOrder,
) : PagingSource<Int, ComicSummary>(), PageInfoReporting {
    override var onPageInfoLoaded: ((totalPages: Int, totalCount: Int) -> Unit)? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ComicSummary> {
        val page = params.key ?: 1

        return try {
            val response = api.getFavouriteComics(sort, page)

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
        fun create(sort: SortOrder): FavouriteComicsPagingSource
    }
}