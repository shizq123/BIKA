package com.shizq.bika.ui.reader.util.preload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.flow.debounce

@Composable
fun <T : Any> PagingPreload(
    pagingItems: LazyPagingItems<T>,
    scrollStateProvider: ScrollStateProvider,
    modelProvider: PreloadModelProvider<T>,
    preloadCount: Int,
    scrollDebounceMillis: Long = 200L
) {
    val context = LocalContext.current

    val preloader = remember(context, modelProvider, preloadCount) {
        val dataProvider = PagingPreloadDataProvider(pagingItems)
        ListPreloader(
            context = context,
            dataProvider = dataProvider,
            modelProvider = modelProvider,
            maxPreload = preloadCount
        )
    }

    LaunchedEffect(preloader, scrollStateProvider) {
        scrollStateProvider.visibleItemsRange
            .debounce(scrollDebounceMillis)
            .collect { visibleRange ->
                if (visibleRange != null) {
                    preloader.onScroll(this, visibleRange.first, visibleRange.last)
                }
            }
    }
}

private class PagingPreloadDataProvider<T : Any>(
    private val pagingItems: LazyPagingItems<T>
) : PreloadDataProvider<T> {
    override val itemCount: Int get() = pagingItems.itemCount
    override fun getItem(index: Int): T? = pagingItems.peek(index)
}