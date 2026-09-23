package com.shizq.bika.feature.reader.impl.util.preload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map


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
        val session = ReaderPreloadSession(
            scope = this,
            dataProvider = PagingPreloadDataProvider(pagingItems),
            modelProvider = modelProvider,
            enqueuer = enqueuer,
            closeEnqueuer = enqueuer::close,
        )
        try {
            // Start immediately, even during continuous scrolling. Also refresh when
            // placeholders receive URLs, without requiring another swipe from the reader.
            val viewportEvents = (scrollStateProvider as? ViewportEventProvider)
                ?.viewportEvents
                ?: scrollStateProvider.visibleItemsRange.map { ViewportSnapshot(it) }
            combine(
                viewportEvents,
                snapshotFlow { pagingItems.itemSnapshotList },
                snapshotFlow { currentPreloadCount },
            ) { viewport, _, count -> viewport to count }
                .collect { (viewport, count) ->
                    session.submitViewport(viewport, count)
                }
        } finally {
            // Closing the session cancels the worker and releases pending downloads.
            session.close()
        }

    }
}

private class PagingPreloadDataProvider<T : Any>(
    private val pagingItems: LazyPagingItems<T>
) : PreloadDataProvider<T> {
    override val itemCount: Int get() = pagingItems.itemCount
    override fun getItem(index: Int): T? = pagingItems.peek(index)
}
