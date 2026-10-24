package com.shizq.bika.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.network.BikaDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/** 接口分页不保证跨页升序，完整目录排序后作为一个 Paging 页面交付。 */
class ChapterListPagingSource @AssistedInject constructor(
    @Assisted private val id: String,
    private val network: BikaDataSource,
) : PagingSource<Int, Chapter>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Chapter> = try {
        val chapters = loadAllChapters { page -> network.getComicEpisodes(id, page).eps }
        LoadResult.Page(
            data = chapters,
            prevKey = null,
            nextKey = null,
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LoadResult.Error(e)
    }

    override fun getRefreshKey(state: PagingState<Int, Chapter>): Int? = null

    @AssistedFactory
    interface Factory {
        fun create(id: String): ChapterListPagingSource
    }
}
