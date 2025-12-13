package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.network.BikaDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ChapterPagesPagingSource @AssistedInject constructor(
    @Assisted private val id: String,
    @Assisted private val order: Int,
    @Assisted private val onMetadataUpdated: (ChapterMeta) -> Unit,
    private val dataSource: BikaDataSource
) : PagingSource<Int, ChapterPage>() {
    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, ChapterPage> {
        return try {
            val currentPage = params.key ?: 1

            val response = dataSource.getChapterPages(id, order, currentPage)

            val paginationData = response.paginationData

            onMetadataUpdated(
                ChapterMeta(
                    title = response.chapterInfo.title,
                    totalImages = response.paginationData.total
                )
            )

            LoadResult.Page(
                data = paginationData.images.map { image ->
                    ChapterPage(id = image.imageId, url = image.media.originalImageUrl)
                },
                prevKey = null,
                nextKey = if (currentPage < paginationData.totalPages) currentPage + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ChapterPage>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            id: String,
            order: Int,
            onMetadataUpdated: (ChapterMeta) -> Unit,
        ): ChapterPagesPagingSource
    }
}

data class ChapterMeta(
    val title: String,
    val totalImages: Int
)

data class ChapterPage(
    val id: String,
    val url: String,
)