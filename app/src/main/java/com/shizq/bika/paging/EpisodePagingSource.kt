package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.core.network.model.nextPageKey
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

            LoadResult.Page(
                data = episodes,
                prevKey = null,
                // 注意用 data.docs 而非去重后的 episodes 判空：整页都是重复项时
                // 页本身是有数据的，停在这里会丢掉后面的章节
                nextKey = data.nextPageKey(currentPage)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Episode>): Int? = null
}