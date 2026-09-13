@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.shizq.bika.feature.reader.impl.util.preload

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
    val enqueuer = remember(context) { CoilPreloadRequestEnqueuer(context) }

    // scrollStateProvider 是 key：它随章节重建（rememberReaderContext 里包了
    // key(chapterOrder)），据此重建 preloader 才能重置其内部的滚动方向状态。
    // 缺了它的话切章后第一次回调会把方向判反，漏掉一轮预载。
    // pagingItems 不能当 key：它整章共用同一个实例，切章时引用不变。
    val preloader = remember(scrollStateProvider, modelProvider, enqueuer, preloadCount) {
        ListPreloader(
            dataProvider = PagingPreloadDataProvider(pagingItems),
            modelProvider = modelProvider,
            enqueuer = enqueuer,
            maxPreload = preloadCount,
        )
    }

    LaunchedEffect(preloader, scrollStateProvider) {
        scrollStateProvider.visibleItemsRange
            .debounce(scrollDebounceMillis)
            .collect { visibleRange ->
                if (visibleRange != null) {
                    preloader.onScroll(visibleRange.first, visibleRange.last)
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
