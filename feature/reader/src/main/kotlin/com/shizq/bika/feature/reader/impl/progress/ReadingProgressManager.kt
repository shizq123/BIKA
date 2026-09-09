package com.shizq.bika.feature.reader.impl.progress

import com.shizq.bika.feature.reader.impl.layout.ReaderController
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
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
    private val config: ProgressConfig = ProgressConfig(),
    private val trackingScope: CoroutineScope,
) {
    /**
     * 当前进度状态
     */
    private val _state = MutableStateFlow<ProgressState>(ProgressState.Idle)
    val state: StateFlow<ProgressState> = _state

    private var persistJob: Job? = null
    private var persistProgress: ((Int) -> Unit)? = null

    /** 页面变化跟踪过程中记录的最后一次页码，供 [persistLastKnownPage] 在组合销毁等同步边界读取。 */
    @Volatile
    private var lastKnownPage: Int = -1

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
                    logger.debug { "恢复成功: 目标=$targetPage 实际=${result.actualPage} 尝试=${result.attempts}次" }
                    ProgressState.Restored(result.actualPage)
                }

                is RestoreResult.Timeout -> {
                    logger.warn { "恢复超时: 目标=$targetPage 降级=${result.fallbackPage}" }
                    ProgressState.Restored(result.fallbackPage)
                }

                is RestoreResult.Failure -> {
                    logger.error { "恢复失败: ${result.reason}" }
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
     */
    fun startTracking(pageFlow: Flow<Int>) {
        trackingScope.launch {
            // 等待恢复完成
            state.first { it is ProgressState.Restored || it is ProgressState.RestoreFailed }
            logger.debug { "恢复完成，开始跟踪页面变化" }

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
    suspend fun persistNow(page: Int, persistProgress: (Int) -> Unit) {
        persistJob?.cancelAndJoin()
        this.persistProgress = persistProgress
        persistProgress(page)
    }

    private fun onPageChanged(page: Int) {
        lastKnownPage = page
        _state.value = ProgressState.Tracking(page)
        logger.info { "页面变化: page=$page" }
        scheduleStore(page)
    }

    /**
     * 组合销毁等同步边界的最终保存：不等待、不挂起，直接用最后一次跟踪到的页码
     * 触发一次 fire-and-forget 持久化（通常是 dispatch 一个 action 到 ViewModel 的队列）。
     *
     * 与 [persistNow] 的区别：[persistNow] 会取消防抖 job 并挂起等待写库完成，
     * 依赖调用方所在的协程作用域在写库完成前保持存活；而 onDispose 这类同步回调
     * 拿到的 `rememberCoroutineScope()` scope 可能在协程被调度前就已取消，
     * 因此这里改为同步调用 [onPersistSync]，让它内部把落库动作转交给不受组合生命周期
     * 影响的作用域（如 ViewModel.viewModelScope）。
     */
    fun persistLastKnownPage(onPersistSync: (Int) -> Unit) {
        if (lastKnownPage >= 0) {
            onPersistSync(lastKnownPage)
        }
    }

    private fun scheduleStore(page: Int) {
        val scope = trackingScope
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(config.persistDebounce)
            persistProgress?.invoke(page)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger("ProgressManager")

    }
}