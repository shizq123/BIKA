package com.shizq.bika.feature.reader.impl.progress

/**
 * 阅读进度的状态机
 */
sealed interface ProgressState {
    /**
     * 空闲状态（初始）
     */
    data object Idle : ProgressState

    /**
     * 正在恢复进度
     * @property targetPage 目标页码
     */
    data class Restoring(val targetPage: Int) : ProgressState

    /**
     * 恢复完成
     * @property actualPage 实际恢复到的页码
     */
    data class Restored(val actualPage: Int) : ProgressState

    /**
     * 恢复失败
     * @property reason 失败原因
     */
    data class RestoreFailed(val reason: String) : ProgressState

    /**
     * 正在跟踪进度变化
     * @property currentPage 当前页码
     */
    data class Tracking(val currentPage: Int) : ProgressState
}
