package com.shizq.bika.ui.comicinfo.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Type
import com.shizq.bika.paging.CrossPageDeduplicator
import com.shizq.bika.paging.forwardOnlyRefreshKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

class CommentPagingSource @AssistedInject constructor(
    private val apiService: BikaDataSource,
    @Assisted private val comicId: String,
    @Assisted private val onTopCommentsLoaded: (List<Comment>) -> Unit,
) : PagingSource<Int, Comment>() {

    /** 跨页重复的 id 在这里拦掉，UI 的 key 才能回归纯 id */
    private val deduplicator = CrossPageDeduplicator<Comment> { it.id }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
        val page = params.key ?: 1

        return try {
            val response = apiService.getComments(Type.COMIC, comicId, page)
            if (page == 1) {
                onTopCommentsLoaded(response.topComments.map { it.asExternalModel() })
            }

            val commentsPage = response.comments

            LoadResult.Page(
                data = deduplicator.retainUnseen(
                    commentsPage.docs.map { it.asExternalModel() }
                ),
                prevKey = null,
                nextKey = if (page >= commentsPage.pages) null else page + 1
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (page == 1) {
                onTopCommentsLoaded(emptyList())
            }
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comment>): Int? =
        state.forwardOnlyRefreshKey()

    @AssistedFactory
    interface Factory {
        operator fun invoke(
            comicId: String,
            onTopCommentsLoaded: (List<Comment>) -> Unit
        ): CommentPagingSource
    }
}