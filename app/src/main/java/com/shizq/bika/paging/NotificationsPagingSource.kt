package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.NotificationDoc
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException

class NotificationsPagingSource @Inject constructor(
    private val api: BikaDataSource
) : PagingSource<Int, NotificationDoc>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NotificationDoc> {
        val page = params.key ?: 1

        return try {
            val response = api.getNotifications(page)
            val notifications = response.notifications

            LoadResult.Page(
                data = notifications.docs,
                prevKey = null,
                nextKey = if (notifications.docs.isEmpty() || page >= notifications.pages) {
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
    override fun getRefreshKey(state: PagingState<Int, NotificationDoc>): Int? = null
}
