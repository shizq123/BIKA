package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Episode

class EpisodePagingSource(
    private val api: BikaDataSource,
    private val comicId: String
) : PagingSource<Int, Episode>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Episode> {
        val currentPage = params.key ?: 1

        return try {
            val response = api.getComicEpisodes(comicId, currentPage)

            val data = response.eps
            val episodes = data.docs

            val nextKey = if (currentPage < data.pages) currentPage + 1 else null

            LoadResult.Page(
                data = episodes,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Episode>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}