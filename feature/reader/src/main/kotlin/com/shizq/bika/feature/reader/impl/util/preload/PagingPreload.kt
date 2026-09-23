package com.shizq.bika.feature.reader.impl.util.preload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.flow.combine

@Composable
fun <T : Any> PagingPreload(
    pagingItems: LazyPagingItems<T>,
    scrollStateProvider: ScrollStateProvider,
    modelProvider: PreloadModelProvider<T>,
    preloadCount: Int,
) {
    val context = LocalContext.current
    val currentPreloadCount by rememberUpdatedState(preloadCount)

    // scrollStateProvider 是 key：它随章节重建（rememberReaderContext 里包了
    // key(chapterOrder)），据此重建 preloader 才能重置其内部的滚动方向状态。
    // 缺了它的话切章后第一次回调会把方向判反，漏掉一轮预载。
    // 不能仅以 pagingItems 为 key：章节切换时它的引用可能不变。
    LaunchedEffect(context, pagingItems, scrollStateProvider, modelProvider) {
        val enqueuer = CoilPreloadRequestEnqueuer(context, this)
        val preloader = ListPreloader(
            dataProvider = PagingPreloadDataProvider(pagingItems),
            modelProvider = modelProvider,
            enqueuer = enqueuer,
            maxPreload = currentPreloadCount,
        )
        try {
            // Start immediately, even during continuous scrolling. Also refresh when
            // placeholders receive URLs, without requiring another swipe from the reader.
            combine(
                scrollStateProvider.visibleItemsRange,
                snapshotFlow { pagingItems.itemSnapshotList },
                snapshotFlow { currentPreloadCount },
            ) { visibleRange, _, count -> visibleRange to count }
                .collect { (visibleRange, count) ->
                    preloader.maxPreload = count
                    if (visibleRange != null) {
                        preloader.onScroll(visibleRange.first, visibleRange.last)
                    } else {
                        enqueuer.updateWindow(emptyList())
                    }
                }
        } finally {
            // Leaving the reader, changing chapter/mode or disabling the window releases
            // pending downloads; overlapping pages within a window keep their progress.
            enqueuer.close()
        }
    }
}

private class PagingPreloadDataProvider<T : Any>(
    private val pagingItems: LazyPagingItems<T>
) : PreloadDataProvider<T> {
    override val itemCount: Int get() = pagingItems.itemCount
    override fun getItem(index: Int): T? = pagingItems.peek(index)
}
