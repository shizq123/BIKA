package com.shizq.bika.feature.reader.impl.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.feature.reader.impl.layout.ReaderController

/**
 * 把 composition 里的 controller / pagingItems 接到 ViewModel 持有的
 * [ReadingProgressManager] 上。
 *
 * 与旧的 rememberReadingProgressManager 的区别：
 * - 不再在这里创建 manager（它归 ViewModel），因此没有「manager 的 scope 是
 *   rememberCoroutineScope」这个根问题
 * - 只有一个 key（[ChapterKey]），恢复与跟踪在同一个 LaunchedEffect 里顺序执行，
 *   不存在旧实现那种「LaunchedEffect(initialPage) 管恢复、LaunchedEffect(manager)
 *   管跟踪、controller 按 chapterOrder 重建」三个 key 各走各路的情况
 * - onDispose 只调 flush()（同步、写入在 viewModelScope），不需要 persistLastKnownPage
 *   那种同步逃生口，也不需要向调用方施加「onPersist 必须不挂起」的契约
 */
@Composable
fun ReadingProgressEffect(
    manager: ReadingProgressManager,
    chapterKey: ChapterKey,
    controller: ReaderController,
    pageItems: LazyPagingItems<ChapterPage>,
    initialPage: Int,
    totalPages: Int,
    chapterTitle: String,
) {
    val dataSource = remember(pageItems) { PagingDataSource(pageItems) }

    // totalPages / chapterTitle 会在 meta 到达后变化，但不该重启恢复会话。
    // 用 rememberUpdatedState 让跟踪协程读到最新值而不作为 key。
    val currentTotalPages by rememberUpdatedState(totalPages)
    val currentTitle by rememberUpdatedState(chapterTitle)

    LaunchedEffect(chapterKey, controller, dataSource) {
        manager.session(
            key = chapterKey,
            targetPage = initialPage,
            totalPagesProvider = { currentTotalPages },
            chapterTitleProvider = { currentTitle },
            dataSource = dataSource,
            controller = controller,
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller, dataSource) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_STOP) {
                manager.flush()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            manager.flush()
        }
    }
}
