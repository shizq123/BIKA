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
import io.github.oshai.kotlinlogging.KotlinLogging
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.feature.reader.impl.layout.ReaderController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger("ReadingProgress")

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
 * @param onPersist 持久化函数（通常是 viewModel::persistProgress）
 * @param config 配置（可选）
 * @return 进度管理器实例
 */
@Composable
fun rememberReadingProgressManager(
    controller: ReaderController,
    imageList: LazyPagingItems<ChapterPage>,
    initialPage: Int,
    onPersist: suspend (Int) -> Boolean,
    config: ProgressConfig = ProgressConfig()
): ReadingProgressManager {
    val scope = rememberCoroutineScope()

    val manager = remember(initialPage) {
        logger.debug { "创建 ProgressManager: initialPage=$initialPage" }
        ReadingProgressManager(
            restoreStrategy = RetryRestoreStrategy(),
            config = config
        )
    }

    val dataSource = remember(imageList) {
        PagingDataSource(imageList)
    }

    // 1. 恢复进度（每次 initialPage 变化时触发）
    LaunchedEffect(initialPage) {
        logger.debug { "开始恢复进度: target=$initialPage" }
        when (val result = manager.restore(initialPage, dataSource, controller)) {
            is RestoreResult.Success -> {
                logger.debug { "恢复成功: page=${result.actualPage}, attempts=${result.attempts}" }
            }
            is RestoreResult.Timeout -> {
                logger.warn { "恢复超时: target=${result.targetPage}, fallback=${result.fallbackPage}" }
            }
            is RestoreResult.Failure -> {
                logger.error { "恢复失败: ${result.reason}" }
            }
        }
    }

    // 2. 跟踪页面变化（manager 创建后立即开始）
    LaunchedEffect(manager) {
        manager.startTracking(
            pageFlow = controller.visibleItemIndex,
            scope = scope,
            persistProgress = onPersist
        )
    }

    // 3. 生命周期感知：在 ON_STOP 时立即保存
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                scope.launch {
                    val currentPage = controller.visibleItemIndex.first()
                    logger.debug { "ON_STOP: 立即保存 page=$currentPage" }
                    manager.persistNow(currentPage)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            scope.launch {
                val currentPage = controller.visibleItemIndex.first()
                logger.debug { "onDispose: 最终保存 page=$currentPage" }
                manager.persistNow(currentPage)
            }
        }
    }

    return manager
}