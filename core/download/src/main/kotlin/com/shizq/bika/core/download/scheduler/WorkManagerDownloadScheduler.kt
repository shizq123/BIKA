package com.shizq.bika.core.download.scheduler

import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class WorkManagerDownloadScheduler @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val workController: DownloadWorkController,
) : DownloadScheduler {

    override suspend fun enqueue(taskId: String) {
        markPendingAndDispatch(taskId, cancelExistingWork = false)
    }

    override suspend fun resume(taskId: String) {
        markPendingAndDispatch(taskId, cancelExistingWork = true)
    }

    /**
     * 将任务置为 PENDING 并触发调度分发。
     *
     * @param cancelExistingWork 是否在触发前先取消当前正在运行的 WorkManager 任务（resume 时需要）
     */
    private suspend fun markPendingAndDispatch(taskId: String, cancelExistingWork: Boolean) {
        val task = repository.getTask(taskId) ?: return
        if (task.status == DownloadStatus.COMPLETED || task.status == DownloadStatus.CANCELED) {
            return
        }

        repository.markPending(
            taskId = taskId,
            nextScheduleAt = System.currentTimeMillis(),
        )

        if (cancelExistingWork) {
            workController.cancelTaskWork(taskId)
        }
        workController.enqueueDispatchWork(replace = true)
    }

    override suspend fun pause(taskId: String) {
        val task = repository.getTask(taskId) ?: return
        if (task.status == DownloadStatus.PAUSED ||
            task.status == DownloadStatus.COMPLETED ||
            task.status == DownloadStatus.CANCELED
        ) {
            return
        }

        repository.markPaused(taskId)
        workController.cancelTaskWork(taskId)
        workController.enqueueDispatchWork(replace = true)
    }

    override suspend fun cancel(taskId: String) {
        val task = repository.getTask(taskId) ?: return
        if (task.status == DownloadStatus.COMPLETED ||
            task.status == DownloadStatus.CANCELED
        ) {
            return
        }

        repository.markCanceled(
            taskId = taskId,
            message = "用户取消下载",
        )
        workController.cancelTaskWork(taskId)
        workController.enqueueDispatchWork(replace = true)
    }

    override suspend fun cancelAndAwaitStopped(
        taskId: String,
        timeoutMs: Long,
    ): Boolean {
        val stopped = workController.cancelTaskAndAwaitStopped(taskId, timeoutMs)
        if (stopped) {
            workController.enqueueDispatchWork(replace = true)
        }
        return stopped
    }

    override suspend fun restorePendingTasks(limit: Int) {
        repository.resetInterruptedTasks()
        workController.enqueueDispatchWork(replace = true)
    }

    override suspend fun onNetworkAvailable() {
        repository.requeueWaitingForNetworkTasks(
            now = System.currentTimeMillis(),
        )
        workController.enqueueDispatchWork(replace = true)
    }
}