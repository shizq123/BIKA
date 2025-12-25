package com.shizq.bika.ui.comicinfo.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.network.BikaDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class CommentPagingSource @AssistedInject constructor(
    private val apiService: BikaDataSource,
    @Assisted private val id: String,
    @Assisted private val onTopCommentsLoaded: (List<Comment>) -> Unit,
) : PagingSource<Int, Comment>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
        val page = params.key ?: 1

        return try {
            val response = apiService.getComicComments(id, page)
            if (page == 1) {
                onTopCommentsLoaded(response.topComments.map { it.asExternalModel() })
            }

            val commentsPage = response.comments

            LoadResult.Page(
                data = commentsPage.docs.map { it.asExternalModel() },
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page >= commentsPage.pages) null else page + 1
            )
        } catch (e: Exception) {
            if (page == 1) {
                onTopCommentsLoaded(emptyList())
            }
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comment>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    @AssistedFactory
    interface Factory {
        operator fun invoke(
            id: String,
            onTopCommentsLoaded: (List<Comment>) -> Unit
        ): CommentPagingSource
    }
}