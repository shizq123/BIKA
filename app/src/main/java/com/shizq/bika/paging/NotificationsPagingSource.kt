package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.NotificationDoc
import jakarta.inject.Inject

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
                nextKey = if (page >= notifications.pages) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, NotificationDoc>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
