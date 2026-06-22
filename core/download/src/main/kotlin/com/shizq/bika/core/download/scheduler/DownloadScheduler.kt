package com.shizq.bika.core.download.scheduler

interface DownloadScheduler {

    /** 新任务入队，若已有未完成 Work，则不重复创建 */
    suspend fun enqueue(taskId: String)

    /** 用户主动恢复，允许替换旧的 backoff / waiting work */
    suspend fun resume(taskId: String)

    /** 用户暂停任务 */
    suspend fun pause(taskId: String)

    /** 用户取消任务 */
    suspend fun cancel(taskId: String)

    /**
     * 仅停止底层 Work，并等待它不再处于 ENQUEUED/RUNNING/BLOCKED。
     * 注意：这个方法不负责更新数据库状态。
     */
    suspend fun cancelAndAwaitStopped(
        taskId: String,
        timeoutMs: Long = 15_000L,
    ): Boolean

    suspend fun restorePendingTasks(limit: Int = Int.MAX_VALUE)

    /** 网络恢复后触发，把 WAITING_FOR_NETWORK 拉回调度 */
    suspend fun onNetworkAvailable()
}