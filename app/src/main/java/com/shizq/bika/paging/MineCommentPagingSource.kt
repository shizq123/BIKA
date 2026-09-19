package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Comment
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
                // docs 为空也停：pages 虚高时否则会一直请求空页
                nextKey = if (comments.docs.isEmpty() || page >= comments.pages) {
                    null
                } else {
                    page + 1
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    /**
     * 固定从第一页重新加载。
     *
     * 不能用"锚点页 nextKey - 1"：本类 prevKey 恒为 null、无法 prepend，
     * 刷新后从中间页起加载，它前面的页既不会被加载也补不回来。
     */
    override fun getRefreshKey(state: PagingState<Int, Comment>): Int? = null
}