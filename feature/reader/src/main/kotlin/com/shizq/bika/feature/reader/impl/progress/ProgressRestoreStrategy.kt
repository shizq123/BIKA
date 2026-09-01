package com.shizq.bika.feature.reader.impl.progress

import io.github.oshai.kotlinlogging.KotlinLogging
import com.shizq.bika.feature.reader.impl.layout.ReaderController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private val logger = KotlinLogging.logger("RetryRestoreStrategy")

/**
 * 进度恢复策略接口
 */
interface ProgressRestoreStrategy {
    /**
     * 执行进度恢复
     * @param targetPage 目标页码
     * @param dataSource 数据源
     * @param controller 阅读控制器
     * @param config 配置
     * @return 恢复结果
     */
    suspend fun restore(
        targetPage: Int,
        dataSource: PageDataSource,
        controller: ReaderController,
        config: ProgressConfig
    ): RestoreResult
}

/**
 * 默认恢复策略：响应式等待数据 + 重试滚动
 *
 * 策略说明：
 * 1. 如果目标页 <= 0，直接成功返回第 0 页
 * 2. 等待初始数据加载（至少 1 页）- 使用 Flow.first 响应式等待
 * 3. 循环执行：
 *    - 尝试滚动到目标页
 *    - 响应式等待数据覆盖目标页（或超时）
 *    - 如果成功，最后一次精准滚动并稳定后返回
 * 4. 超时后降级到已加载的最大页
 */
class RetryRestoreStrategy : ProgressRestoreStrategy {

    override suspend fun restore(
        targetPage: Int,
        dataSource: PageDataSource,
        controller: ReaderController,
        config: ProgressConfig
    ): RestoreResult {
        // 目标页无效，直接返回第 0 页
        if (targetPage <= 0) {
            logger.debug { "目标页 <= 0，直接返回第 0 页" }
            return RestoreResult.Success(actualPage = 0, attempts = 0)
        }

        // 第一阶段：等待初始数据加载（响应式）
        logger.debug { "等待初始数据加载..." }
        val initialLoaded = withTimeoutOrNull(config.initialLoadTimeout) {
            dataSource.loadedCountFlow.first { it >= 1 }
        }
        if (initialLoaded == null) {
            return RestoreResult.Failure("初始数据加载超时")
        }

        // 第二阶段：循环滚动 + 响应式等待数据
        logger.debug { "开始恢复到目标页: $targetPage" }
        var attempts = 0
        val maxAttempts = (config.restoreTimeout.inWholeMilliseconds / config.retryInterval.inWholeMilliseconds).toInt()

        repeat(maxAttempts) {
            attempts++
            controller.scrollToPage(targetPage)

            val loadedCount = dataSource.loadedCount
            logger.info { "尝试 #$attempts: 目标=$targetPage, 已加载=$loadedCount" }

            // 响应式等待数据覆盖目标页
            val dataReady = withTimeoutOrNull(config.retryInterval) {
                dataSource.loadedCountFlow.first { it > targetPage }
            }

            if (dataReady != null) {
                // 数据已到位，最后一次精准滚动
                delay(config.stabilizeDelay)
                controller.scrollToPage(targetPage)

                logger.debug { "恢复成功: 目标=$targetPage, 尝试=$attempts 次" }
                return RestoreResult.Success(
                    actualPage = targetPage,
                    attempts = attempts
                )
            }
        }

        // 超时：降级到已加载的最大页
        val fallbackPage = (dataSource.loadedCount - 1).coerceAtLeast(0)
        logger.warn { "恢复超时: 目标=$targetPage, 降级到=$fallbackPage, 尝试=$attempts 次" }
        
        return RestoreResult.Timeout(
            targetPage = targetPage,
            fallbackPage = fallbackPage,
            attempts = attempts
        )
    }

    private companion object {
        const val TAG = "RetryRestoreStrategy"
    }
}
