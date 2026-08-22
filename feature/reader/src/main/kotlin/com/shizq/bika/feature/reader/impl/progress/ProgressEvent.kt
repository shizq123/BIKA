package com.shizq.bika.feature.reader.impl.progress

import kotlin.time.Clock

/**
 * 进度管理事件，用于审计、调试和 Analytics
 */
sealed interface ProgressEvent {
    val timestamp: Long get() = Clock.System.now().toEpochMilliseconds()
    val metadata: Map<String, Any> get() = emptyMap()

    // ===== 恢复事件 =====
    data class RestoreStarted(
        val chapterOrder: Int,
        val targetPage: Int
    ) : ProgressEvent

    data class RestoreSucceeded(
        val chapterOrder: Int,
        val targetPage: Int,
        val elapsedMs: Long,
        val retryCount: Int
    ) : ProgressEvent {
        override val metadata: Map<String, Any>
            get() = mapOf(
                "elapsed_ms" to elapsedMs,
                "retry_count" to retryCount
            )
    }

    data class RestoreFailed(
        val chapterOrder: Int,
        val targetPage: Int,
        val reason: FailureReason,
        val elapsedMs: Long
    ) : ProgressEvent {
        override val metadata: Map<String, Any>
            get() = mapOf(
                "reason" to reason.name,
                "elapsed_ms" to elapsedMs
            )
    }

    // ===== 保存事件 =====
    data class StoreStarted(
        val chapterOrder: Int,
        val page: Int,
        val isUrgent: Boolean
    ) : ProgressEvent

    data class StoreSucceeded(
        val chapterOrder: Int,
        val page: Int,
        val isUrgent: Boolean,
        val elapsedMs: Long
    ) : ProgressEvent {
        override val metadata: Map<String, Any>
            get() = mapOf(
                "is_urgent" to isUrgent,
                "elapsed_ms" to elapsedMs
            )
    }

    data class StoreFailed(
        val chapterOrder: Int,
        val page: Int,
        val isUrgent: Boolean,
        val error: Throwable
    ) : ProgressEvent {
        override val metadata: Map<String, Any>
            get() = mapOf(
                "is_urgent" to isUrgent,
                "error" to error.message.orEmpty()
            )
    }

    // ===== 同步事件 =====
    data class SyncStarted(
        val fromChapter: Int,
        val toChapter: Int,
        val fromPage: Int
    ) : ProgressEvent

    data class SyncCompleted(
        val fromChapter: Int,
        val toChapter: Int,
        val fromPage: Int
    ) : ProgressEvent

    /**
     * 失败原因枚举
     */
    enum class FailureReason {
        TIMEOUT,           // 超时（15秒内数据未加载到位）
        DATA_NOT_READY,    // 数据源为空或占位符
        SCROLL_ERROR,      // 滚动异常
        INVALID_PAGE,      // 页码超出范围
        UNKNOWN           // 未知错误
    }
}
