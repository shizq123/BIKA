package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.network.BikaDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ChapterListPagingSource @AssistedInject constructor(
    @Assisted private val id: String,
    private val network: BikaDataSource
) : PagingSource<Int, Chapter>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Chapter> {
        return try {
            val currentPage = params.key ?: 1

            val response = network.getComicEpisodes(id, currentPage)
            val data = response.eps

            LoadResult.Page(
                data = data.docs.map { doc ->
                    Chapter(doc.id, doc.order, doc.title, doc.updatedAt)
                },
                prevKey = null,
                nextKey = if (currentPage < data.pages) currentPage + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

    }

    override fun getRefreshKey(state: PagingState<Int, Chapter>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(id: String): ChapterListPagingSource
    }
}

data class Chapter(
    val id: String,
    val order: Int,
    val title: String,
    val updatedAt: String
)