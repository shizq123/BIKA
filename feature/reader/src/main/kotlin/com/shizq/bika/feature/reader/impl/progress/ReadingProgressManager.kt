package com.shizq.bika.feature.reader.impl.progress

import com.shizq.bika.core.common.BikaLog
import com.shizq.bika.feature.reader.impl.layout.ReaderController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 阅读进度管理器
 *
 * 职责：
 * 1. 协调进度恢复
 * 2. 跟踪页面变化
 * 3. 防抖保存进度
 * 4. 关键时机立即保存
 *
 * 使用示例：
 * ```
 * val manager = ReadingProgressManager(
 *     restoreStrategy = RetryRestoreStrategy(),
 *     config = ProgressConfig()
 * )
 *
 * // 恢复进度
 * manager.restore(targetPage, dataSource, controller)
 *
 * // 开始跟踪
 * manager.startTracking(pageFlow, scope) { page ->
 *     viewModel.persistProgress(page)
 * }
 *
 * // 立即保存
 * manager.persistNow(currentPage)
 * ```
 */
class ReadingProgressManager(
    private val restoreStrategy: ProgressRestoreStrategy,
    private val config: ProgressConfig = ProgressConfig()
) {
    /**
     * 当前进度状态
     */
    private val _state = MutableStateFlow<ProgressState>(ProgressState.Idle)
    val state: StateFlow<ProgressState> = _state

    private var persistJob: Job? = null
    private var persistProgress: (suspend (Int) -> Boolean)? = null

    /**
     * 恢复到目标页
     *
     * @param targetPage 目标页码
     * @param dataSource 页面数据源
     * @param controller 阅读控制器
     * @return 恢复结果
     */
    suspend fun restore(
        targetPage: Int,
        dataSource: PageDataSource,
        controller: ReaderController
    ): RestoreResult {
        _state.value = ProgressState.Restoring(targetPage)

        return restoreStrategy.restore(targetPage, dataSource, controller, config).also { result ->
            _state.value = when (result) {
                is RestoreResult.Success -> {
                    BikaLog.d(
                        TAG,
                        "恢复成功: 目标=$targetPage 实际=${result.actualPage} 尝试=${result.attempts}次"
                    )
                    ProgressState.Restored(result.actualPage)
                }

                is RestoreResult.Timeout -> {
                    BikaLog.w(TAG, "恢复超时: 目标=$targetPage 降级=${result.fallbackPage}")
                    ProgressState.Restored(result.fallbackPage)
                }

                is RestoreResult.Failure -> {
                    BikaLog.e(TAG, "恢复失败: ${result.reason}")
                    ProgressState.RestoreFailed(result.reason)
                }
            }
        }
    }

    /**
     * 开始跟踪页面变化
     *
     * 注意：
     * - 会等待恢复完成后才开始跟踪，避免恢复过程中的误保存
     * - 使用 distinctUntilChanged 避免重复保存相同页码
     *
     * @param pageFlow 页码变化流（通常是 controller.visibleItemIndex）
     * @param scope 协程作用域
     * @param persistProgress 持久化函数（返回是否成功）
     */
    fun startTracking(
        pageFlow: Flow<Int>,
        scope: CoroutineScope,
        persistProgress: suspend (Int) -> Boolean
    ) {
        this.persistProgress = persistProgress

        scope.launch {
            // 等待恢复完成
            state.first { it is ProgressState.Restored || it is ProgressState.RestoreFailed }
            BikaLog.d(TAG, "恢复完成，开始跟踪页面变化")

            // 开始跟踪
            pageFlow
                .distinctUntilChanged()
                .collect { page ->
                    onPageChanged(page)
                }
        }
    }

    /**
     * 立即保存进度（取消防抖，同步保存）
     *
     * 用于关键时机：返回、退后台、销毁等
     *
     * @param page 当前页码
     * @return 是否保存成功
     */
    suspend fun persistNow(page: Int): Boolean {
        persistJob?.cancelAndJoin()
        val result = persistProgress?.invoke(page) ?: false
        if (result) {
            BikaLog.d(TAG, "立即保存成功: page=$page")
        } else {
            BikaLog.w(TAG, "立即保存失败: page=$page")
        }
        return result
    }

    private fun onPageChanged(page: Int) {
        _state.value = ProgressState.Tracking(page)
        BikaLog.i(TAG, "页面变化: page=$page")
        scheduleStore(page)
    }

    private fun scheduleStore(page: Int) {
        persistJob?.cancel()
        persistJob = CoroutineScope(Dispatchers.IO).launch {
            delay(config.persistDebounce)
            val result = persistProgress?.invoke(page) ?: false
            if (result) {
                BikaLog.d(TAG, "防抖保存成功: page=$page")
            } else {
                BikaLog.w(TAG, "防抖保存失败: page=$page")
            }
        }
    }

    private companion object {
        const val TAG = "ProgressManager"
    }
}