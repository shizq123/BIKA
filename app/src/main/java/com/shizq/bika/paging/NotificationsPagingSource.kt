package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.NotificationDoc
import com.shizq.bika.core.network.model.nextPageKey
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
                nextKey = notifications.nextPageKey(page),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, NotificationDoc>): Int? = null
}
