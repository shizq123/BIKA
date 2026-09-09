package com.shizq.bika.feature.reader.impl.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.feature.reader.impl.layout.ReaderController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 在 Compose 中创建并管理 ReadingProgressManager
 *
 * 功能：
 * 1. 自动恢复到 initialPage
 * 2. 跟踪页面变化并防抖保存
 * 3. 在关键时机（ON_STOP、onDispose）立即保存
 *
 * @param controller 阅读控制器
 * @param imageList 页面数据（Paging）
 * @param initialPage 初始页码
 * @param onPersist 持久化函数：不挂起、立即返回，内部应将落库动作转交给不受组合生命周期
 *   影响的作用域（如 ViewModel.viewModelScope），而不是自己去写库。这一约束同时覆盖了
 *   页面跟踪防抖保存、ON_STOP、以及组合销毁（onDispose）三条调用路径——onDispose 里拿到的
 *   `rememberCoroutineScope()` 协程作用域会在组合销毁过程中被取消，提交给它的挂起任务
 *   不保证能在取消生效前被调度执行，因此这里统一要求同步回调。
 * @param config 配置（可选）
 * @return 进度管理器实例
 */
@Composable
fun rememberReadingProgressManager(
    controller: ReaderController,
    imageList: LazyPagingItems<ChapterPage>,
    initialPage: Int,
    onPersist: (Int) -> Unit,
    config: ProgressConfig = ProgressConfig()
): ReadingProgressManager {
    val scope = rememberCoroutineScope()

    val manager = remember(initialPage) {
        ReadingProgressManager(
            restoreStrategy = RetryRestoreStrategy(),
            config = config,
            trackingScope = scope,
        )
    }

    val dataSource = remember(imageList) {
        PagingDataSource(imageList)
    }

    // 1. 恢复进度（每次 initialPage 变化时触发）
    LaunchedEffect(initialPage) {
        when (manager.restore(initialPage, dataSource, controller)) {
            is RestoreResult.Success -> {
            }
            is RestoreResult.Timeout -> {
            }
            is RestoreResult.Failure -> {
            }
        }
    }

    // 2. 跟踪页面变化（manager 创建后立即开始）
    LaunchedEffect(manager) {
        manager.startTracking(pageFlow = controller.visibleItemIndex)
    }

    // 3. 生命周期感知：在 ON_STOP 时立即保存（此时组合仍在，scope 未被取消，可安全挂起等待）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                scope.launch {
                    val currentPage = controller.visibleItemIndex.first()
                    manager.persistNow(currentPage, onPersist)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            manager.persistLastKnownPage { page ->
                onPersist(page)
            }
        }
    }

    return manager
}