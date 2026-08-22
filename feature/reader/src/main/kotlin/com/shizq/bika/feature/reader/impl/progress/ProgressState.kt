package com.shizq.bika.feature.reader.impl.progress

import androidx.compose.runtime.Immutable

/**
 * 进度管理状态机
 * 
 * 通过显式状态类型确保同一时刻只能处于一种状态，
 * 避免"一边恢复一边保存"的竞态问题。
 */
@Immutable
sealed interface ProgressState {
    /**
     * 空闲状态：未进行任何进度操作
     */
    data object Idle : ProgressState

    /**
     * 恢复中：正在将页面滚动到上次保存的位置
     * @param targetPage 目标恢复页码
     * @param startTime 恢复开始时间戳（毫秒）
     * @param maxRetries 最大重试次数（用于 Paging 加载等待）
     */
    data class Restoring(
        val targetPage: Int,
        val startTime: Long = System.currentTimeMillis(),
        val maxRetries: Int = 150
    ) : ProgressState {
        fun hasTimedOut(currentTime: Long = System.currentTimeMillis()): Boolean {
            return currentTime - startTime > 15_000L // 15秒超时
        }

        fun shouldContinueRetrying(attemptCount: Int): Boolean {
            return attemptCount < maxRetries && !hasTimedOut()
        }
    }

    /**
     * 保存中：正在将当前页码写入数据库
     * @param page 待保存的页码
     * @param isUrgent 是否紧急保存（退后台/销毁等关键时机需立即同步写库）
     */
    data class Saving(
        val page: Int,
        val isUrgent: Boolean = false
    ) : ProgressState

    /**
     * 跨章同步：切章前先保存当前章进度，确保不丢失
     * @param fromChapter 当前章节 order
     * @param fromPage 当前页码
     * @param toChapter 目标章节 order
     * @param startFromBeginning 是否从头开始（不恢复目标章历史进度）
     */
    data class Syncing(
        val fromChapter: Int,
        val fromPage: Int,
        val toChapter: Int,
        val startFromBeginning: Boolean = false
    ) : ProgressState
}
