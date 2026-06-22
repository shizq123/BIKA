package com.shizq.bika.core.download.scheduler

interface DownloadWorkController {
    fun enqueueTaskWork(taskId: String, replace: Boolean = false)
    fun cancelTaskWork(taskId: String)

    fun enqueueDispatchWork(
        delayMs: Long = 0L,
        replace: Boolean = true,
    )

    suspend fun cancelTaskAndAwaitStopped(
        taskId: String,
        timeoutMs: Long = 15_000L,
    ): Boolean
}