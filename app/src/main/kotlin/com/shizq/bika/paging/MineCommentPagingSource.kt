package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Comment
import com.shizq.bika.core.network.model.nextPageKey
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException

class MineCommentPagingSource @Inject constructor(
    private val api: BikaDataSource
) : PagingSource<Int, Comment>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
        val page = params.key ?: 1

        return try {
            val response = api.getMyComments(page)

            val comments = response.comments

            LoadResult.Page(
                data = comments.docs,
                prevKey = null,
                nextKey = comments.nextPageKey(page),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comment>): Int? = null
}