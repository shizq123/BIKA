package com.shizq.bika.feature.reader.impl.progress

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.common.BikaLog
import com.shizq.bika.core.data.paging.ChapterMeta
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.feature.reader.impl.layout.ReaderController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

/**
 * 阅读进度管理器
 * 
 * 职责：
 * 1. 恢复进度：等待数据加载，滚动到目标页
 * 2. 保存进度：根据策略（debounce/立即）写入数据库
 * 3. 同步进度：切章前保存当前章进度
 * 4. 状态管理：通过状态机防止并发冲突
 * 5. 事件追踪：发出审计事件供日志/Analytics
 */
class ProgressManager(
    private val asyncStoreStrategy: StoreStrategy,
    private val immediateStoreStrategy: StoreStrategy
) {
    val state: StateFlow<ProgressState>
        field = MutableStateFlow<ProgressState>(ProgressState.Idle)

    val events: SharedFlow<ProgressEvent>
        field = MutableSharedFlow<ProgressEvent>(replay = 0, extraBufferCapacity = 64)

    // 互斥锁：确保同一时刻只能执行一个进度操作（恢复/保存/同步）
    private val mutex = Mutex()

    // 缓存失败的保存请求，下次成功时重试
    private val pendingSaves = mutableListOf<SaveRequest>()

    private data class SaveRequest(
        val comicId: String,
        val chapterOrder: Int,
        val meta: ChapterMeta?,
        val pageIndex: Int,
        val isUrgent: Boolean
    )

    companion object {
        private const val TAG = "ProgressManager"
    }

    /**
     * 恢复进度：滚动到上次保存的位置
     * 
     * @param targetPage 目标页码
     * @param chapterOrder 章节号（用于日志）
     * @param imageList Paging 数据源
     * @param controller 阅读器控制器
     * @param lazyListState 条漫模式的 LazyListState（翻页模式为 null）
     */
    suspend fun restoreProgress(
        targetPage: Int,
        chapterOrder: Int,
        imageList: LazyPagingItems<ChapterPage>,
        controller: ReaderController,
        lazyListState: LazyListState?
    ): Result<Unit> = mutex.withLock {
        if (targetPage <= 0) {
            BikaLog.d(TAG, "跳过恢复: targetPage=$targetPage <= 0")
            return@withLock Result.success(Unit)
        }

        val restoringState = ProgressState.Restoring(targetPage = targetPage)
        state.value = restoringState
        events.emit(ProgressEvent.RestoreStarted(chapterOrder, targetPage))

        val startTime = System.currentTimeMillis()
        var retryCount = 0

        return@withLock try {
            // 1. 等待数据流就位（itemCount > 0）
            snapshotFlow { imageList.itemCount }.first { it > 0 }

            BikaLog.d(
                TAG,
                "数据流就位, itemCount=${imageList.itemCount}, 开始定位 targetPage=$targetPage"
            )

            // 2. 根据模式滚动（条漫 vs 翻页）
            if (lazyListState != null) {
                // 条漫模式：循环滚动直到数据批次覆盖目标页
                while (restoringState.shouldContinueRetrying(retryCount)) {
                    lazyListState.scrollToItem(targetPage)
                    if (imageList.itemCount > targetPage) break
                    delay(100.milliseconds)
                    retryCount++
                }
                if (imageList.itemCount <= targetPage) {
                    throw RestoreTimeoutException("条漫恢复超时: targetPage=$targetPage itemCount=${imageList.itemCount}")
                }
                val visibleIndexes = lazyListState.layoutInfo.visibleItemsInfo.map { it.index }
                BikaLog.d(TAG, "条漫恢复完成: 目标=$targetPage, 当前可见=$visibleIndexes")
                delay(300.milliseconds) // 等待布局稳定
            } else {
                // 翻页模式：同理循环滚动
                while (restoringState.shouldContinueRetrying(retryCount)) {
                    controller.scrollToPage(targetPage)
                    if (imageList.itemCount > targetPage) break
                    delay(100.milliseconds)
                    retryCount++
                }
                if (imageList.itemCount <= targetPage) {
                    throw RestoreTimeoutException("翻页恢复超时: targetPage=$targetPage itemCount=${imageList.itemCount}")
                }
                delay(300.milliseconds)
            }

            val elapsedMs = System.currentTimeMillis() - startTime
            events.emit(
                ProgressEvent.RestoreSucceeded(
                    chapterOrder,
                    targetPage,
                    elapsedMs,
                    retryCount
                )
            )
            BikaLog.d(
                TAG,
                "恢复成功: chapterOrder=$chapterOrder targetPage=$targetPage 耗时=${elapsedMs}ms 重试=$retryCount"
            )

            // 恢复成功后延迟释放状态，确保 debounce 自动保存协程感知到恢复状态
            delay(1500.milliseconds)
            state.value = ProgressState.Idle
            Result.success(Unit)
        } catch (e: Exception) {
            val elapsedMs = System.currentTimeMillis() - startTime
            val reason = when (e) {
                is RestoreTimeoutException -> ProgressEvent.FailureReason.TIMEOUT
                else -> ProgressEvent.FailureReason.UNKNOWN
            }
            events.emit(ProgressEvent.RestoreFailed(chapterOrder, targetPage, reason, elapsedMs))
            BikaLog.e(TAG, "恢复失败: chapterOrder=$chapterOrder targetPage=$targetPage", e)
            state.value = ProgressState.Idle
            Result.failure(e)
        }
    }

    /**
     * 保存进度（异步，debounce 后自动保存）
     */
    suspend fun saveProgressAsync(
        comicId: String,
        chapterOrder: Int,
        meta: ChapterMeta?,
        pageIndex: Int
    ): Result<Unit> {
        // 恢复期间不保存
        if (state.value is ProgressState.Restoring) {
            BikaLog.d(TAG, "跳过自动保存: 正在恢复进度")
            return Result.success(Unit)
        }

        return executeSave(
            comicId,
            chapterOrder,
            meta,
            pageIndex,
            isUrgent = false,
            asyncStoreStrategy
        )
    }

    /**
     * 保存进度（立即，紧急时机用）
     * 返回键、退后台、销毁等场景
     */
    suspend fun saveProgressImmediate(
        comicId: String,
        chapterOrder: Int,
        meta: ChapterMeta?,
        pageIndex: Int
    ): Result<Unit> {
        // 恢复期间不保存
        if (state.value is ProgressState.Restoring) {
            BikaLog.d(TAG, "跳过紧急保存: 正在恢复进度")
            return Result.success(Unit)
        }

        return executeSave(
            comicId,
            chapterOrder,
            meta,
            pageIndex,
            isUrgent = true,
            immediateStoreStrategy
        )
    }

    /**
     * 跨章同步：切章前保存当前章进度
     */
    suspend fun syncBeforeChapterChange(
        comicId: String,
        fromChapter: Int,
        fromPage: Int,
        toChapter: Int,
        meta: ChapterMeta?
    ): Result<Unit> = mutex.withLock {
        state.value = ProgressState.Syncing(fromChapter, fromPage, toChapter)
        events.emit(ProgressEvent.SyncStarted(fromChapter, toChapter, fromPage))

        return@withLock try {
            // 同步用立即保存策略（确保切章前进度已落库）
            val result = immediateStoreStrategy.store(comicId, fromChapter, meta, fromPage)
            if (result.isSuccess) {
                events.emit(ProgressEvent.SyncCompleted(fromChapter, toChapter, fromPage))
                BikaLog.d(TAG, "跨章同步成功: from=$fromChapter to=$toChapter page=$fromPage")
            }
            state.value = ProgressState.Idle
            result
        } catch (e: Exception) {
            BikaLog.e(TAG, "跨章同步失败: from=$fromChapter to=$toChapter", e)
            state.value = ProgressState.Idle
            Result.failure(e)
        }
    }

    private suspend fun executeSave(
        comicId: String,
        chapterOrder: Int,
        meta: ChapterMeta?,
        pageIndex: Int,
        isUrgent: Boolean,
        strategy: StoreStrategy
    ): Result<Unit> {
        state.update { ProgressState.Saving(pageIndex, isUrgent) }
        events.emit(ProgressEvent.StoreStarted(chapterOrder, pageIndex, isUrgent))

        val startTime = System.currentTimeMillis()
        val result = strategy.store(comicId, chapterOrder, meta, pageIndex)

        val elapsedMs = System.currentTimeMillis() - startTime
        if (result.isSuccess) {
            events.emit(ProgressEvent.StoreSucceeded(chapterOrder, pageIndex, isUrgent, elapsedMs))
            BikaLog.d(
                TAG,
                "保存成功: chapter=$chapterOrder page=$pageIndex urgent=$isUrgent 耗时=${elapsedMs}ms"
            )

            // 保存成功后，重试之前失败的请求
            if (pendingSaves.isNotEmpty()) {
                BikaLog.d(TAG, "重试 ${pendingSaves.size} 个待保存请求")
                val toRetry = pendingSaves.toList()
                pendingSaves.clear()
                toRetry.forEach { req ->
                    val retryStrategy =
                        if (req.isUrgent) immediateStoreStrategy else asyncStoreStrategy
                    retryStrategy.store(req.comicId, req.chapterOrder, req.meta, req.pageIndex)
                }
            }
        } else {
            events.emit(
                ProgressEvent.StoreFailed(
                    chapterOrder, pageIndex, isUrgent,
                    result.exceptionOrNull() ?: Exception("Unknown error")
                )
            )
            BikaLog.e(TAG, "保存失败: chapter=$chapterOrder page=$pageIndex urgent=$isUrgent")

            // 失败则缓存，下次成功时重试
            pendingSaves.add(SaveRequest(comicId, chapterOrder, meta, pageIndex, isUrgent))
        }

        state.value = ProgressState.Idle
        return result
    }

    /**
     * 是否正在恢复进度
     */
    fun isRestoring(): Boolean = state.value is ProgressState.Restoring
}

/**
 * 恢复超时异常
 */
private class RestoreTimeoutException(message: String) : Exception(message)

