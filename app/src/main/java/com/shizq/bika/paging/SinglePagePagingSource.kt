package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlin.coroutines.cancellation.CancellationException

@Suppress("FunctionName")
fun <Key : Any, V : Any> SinglePagePagingSource(
    fetcher: suspend () -> List<V>
): PagingSource<Key, V> {
    return object : PagingSource<Key, V>() {
        override suspend fun load(params: LoadParams<Key>): LoadResult<Key, V> {
            return try {
                val data = fetcher()
                LoadResult.Page(
                    data = data,
                    prevKey = null,
                    nextKey = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Key, V>): Key? {
            return state.anchorPosition?.let { anchorPosition ->
                state.closestPageToPosition(anchorPosition)?.prevKey
                    ?: state.closestPageToPosition(anchorPosition)?.nextKey
            }
        }
    }
}