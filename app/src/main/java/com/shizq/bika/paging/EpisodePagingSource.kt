package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Episode
import kotlinx.coroutines.CancellationException

class EpisodePagingSource(
    private val api: BikaDataSource,
    private val comicId: String
) : PagingSource<Int, Episode>() {

    private val deduplicator = CrossPageDeduplicator<Episode> { it.id }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Episode> {
        val currentPage = params.key ?: 1

        return try {
            val response = api.getComicEpisodes(comicId, currentPage)

            val data = response.eps
            val episodes = deduplicator.retainUnseen(currentPage, data.docs)

            val nextKey = if (data.docs.isEmpty() || currentPage >= data.pages) {
                null
            } else {
                currentPage + 1
            }

            LoadResult.Page(
                data = episodes,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Episode>): Int? = null
}