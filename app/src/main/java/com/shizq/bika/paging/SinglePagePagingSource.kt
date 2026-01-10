package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlin.coroutines.cancellation.CancellationException

abstract class SinglePagePagingSource<Key : Any, V : Any> : PagingSource<Key, V>() {
    final override fun getRefreshKey(state: PagingState<Key, V>): Key? = null
}

@Suppress("FunctionName")
fun <Key : Any, V : Any> SinglePagePagingSource(
    load: suspend PagingSource<Key, V>.(params: PagingSource.LoadParams<Key>) -> PagingSource.LoadResult<Key, V>
): PagingSource<Key, V> {
    @Suppress("UnnecessaryVariable", "RedundantSuppression")
    val load1 = load
    return object : PagingSource<Key, V>() {
        override suspend fun load(params: LoadParams<Key>): LoadResult<Key, V> {
            return try {
                load1(params)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Key, V>): Key? = null
    }
}