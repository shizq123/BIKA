package com.shizq.bika.feature.reader.impl.progress

/**
 * 进度恢复的结果
 */
sealed interface RestoreResult {
    /**
     * 恢复成功
     * @property actualPage 实际恢复到的页码
     * @property attempts 尝试次数
     */
    data class Success(
        val actualPage: Int,
        val attempts: Int = 0
    ) : RestoreResult

    /**
     * 恢复超时（降级到已加载的最大页）
     * @property targetPage 目标页码
     * @property fallbackPage 降级到的页码
     * @property attempts 尝试次数
     */
    data class Timeout(
        val targetPage: Int,
        val fallbackPage: Int,
        val attempts: Int
    ) : RestoreResult

    /**
     * 恢复失败
     * @property reason 失败原因
     */
    data class Failure(val reason: String) : RestoreResult
}
