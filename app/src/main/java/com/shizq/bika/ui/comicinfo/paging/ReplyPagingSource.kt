package com.shizq.bika.ui.comicinfo.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.nextPageKey
import com.shizq.bika.paging.CrossPageDeduplicator
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * 某条评论下的回复列表。
 *
 * 原实现调的是 `getComments(Type.COMIC, id, page)`，拼出 `comics/{commentId}/comments/`
 * —— 把评论 id 当成漫画 id 用，服务端不可能返回这条评论的回复。
 * 正确的端点是 [BikaDataSource.getCommentReplies]（`comments/{id}/childrens/`）。
 */
class ReplyPagingSource @AssistedInject constructor(
    private val api: BikaDataSource,
    @Assisted private val commentId: String,
) : PagingSource<Int, Comment>() {

    /** 跨页重复的 id 在这里拦掉，UI 的 key 才能回归纯 id */
    private val deduplicator = CrossPageDeduplicator<Comment> { it.id }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
        val page = params.key ?: 1

        return try {
            val response = api.getCommentReplies(commentId, page)

            val commentsPage = response.comments

            LoadResult.Page(
                data = deduplicator.retainUnseen(
                    page,
                    commentsPage.docs.map { it.asExternalModel() }
                ),
                prevKey = null,
                nextKey = commentsPage.nextPageKey(page),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comment>): Int? = null

    @AssistedFactory
    interface Factory {
        operator fun invoke(commentId: String): ReplyPagingSource
    }
}
