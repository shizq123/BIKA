package com.shizq.bika.feature.reader.impl.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.common.BikaLog
import com.shizq.bika.core.data.paging.ChapterMeta
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.feature.reader.impl.layout.ReaderContext
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.runBlocking

/**
 * Compose 层进度管理集成
 * 
 * 将 ProgressManager 集成到 Composable 函数中，
 * 处理恢复、自动保存、生命周期保存等逻辑
 */

/**
 * 进度恢复 Effect
 * 
 * 当章节切换或初始页变化时，自动恢复到上次阅读位置
 */
@Composable
fun ProgressRestoreEffect(
    progressManager: ProgressManager,
    chapterOrder: Int,
    initialPage: Int,
    imageList: LazyPagingItems<ChapterPage>,
    readerContext: ReaderContext
) {
    LaunchedEffect(chapterOrder, initialPage) {
        if (initialPage <= 0) return@LaunchedEffect

        progressManager.restoreProgress(
            targetPage = initialPage,
            chapterOrder = chapterOrder,
            imageList = imageList,
            controller = readerContext.controller,
            lazyListState = readerContext.lazyListState
        )
    }
}

/**
 * 自动保存 Effect
 * 
 * 监听页面变化，debounce 1秒后自动保存进度
 */
@Composable
fun AutoSaveEffect(
    progressManager: ProgressManager,
    comicId: String,
    chapterOrder: Int,
    meta: ChapterMeta?,
    readerContext: ReaderContext
) {
    LaunchedEffect(readerContext.controller) {
        readerContext.controller.visibleItemIndex
            .debounce(1000)
            .collect { pageIndex ->
                progressManager.saveProgressAsync(
                    comicId = comicId,
                    chapterOrder = chapterOrder,
                    meta = meta,
                    pageIndex = pageIndex
                )
            }
    }
}

/**
 * 生命周期保存 Effect
 * 
 * 在以下关键时机立即保存进度：
 * - 应用退后台（ON_STOP）
 * - 组件销毁（onDispose）
 */
@Composable
fun LifecycleSaveEffect(
    progressManager: ProgressManager,
    comicId: String,
    chapterOrder: Int,
    meta: ChapterMeta?,
    getCurrentPage: () -> Int
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !progressManager.isRestoring()) {
                val currentPage = getCurrentPage()
                BikaLog.d("ProgressManager", "ON_STOP 退后台立即保存 page=$currentPage")
                // 使用 runBlocking 确保退后台前保存完成
                runBlocking {
                    progressManager.saveProgressImmediate(
                        comicId = comicId,
                        chapterOrder = chapterOrder,
                        meta = meta,
                        pageIndex = currentPage
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!progressManager.isRestoring()) {
                val currentPage = getCurrentPage()
                BikaLog.d("ProgressManager", "onDispose 组件销毁立即保存 page=$currentPage")
                runBlocking {
                    progressManager.saveProgressImmediate(
                        comicId = comicId,
                        chapterOrder = chapterOrder,
                        meta = meta,
                        pageIndex = currentPage
                    )
                }
            }
        }
    }
}

/**
 * 事件监听 Effect
 * 
 * 收集 ProgressManager 发出的事件，用于日志或 Analytics
 */
@Composable
fun ProgressEventCollector(
    progressManager: ProgressManager,
    onEvent: (ProgressEvent) -> Unit = { event ->
        // 默认只记录失败事件
        when (event) {
            is ProgressEvent.RestoreFailed -> {
                BikaLog.w("ProgressEvent", "恢复失败: ${event.reason} chapter=${event.chapterOrder} page=${event.targetPage}")
            }
            is ProgressEvent.StoreFailed -> {
                BikaLog.w("ProgressEvent", "保存失败: ${event.error.message} chapter=${event.chapterOrder} page=${event.page}")
            }
            else -> {
                // 成功事件可选择性记录
            }
        }
    }
) {
    LaunchedEffect(progressManager) {
        progressManager.events.collect { event ->
            onEvent(event)
        }
    }
}

