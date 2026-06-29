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

    // 不可调度的终态，重复校验时统一引用
    private val terminalStatuses = setOf(DownloadStatus.COMPLETED, DownloadStatus.CANCELED)

    override suspend fun enqueue(taskId: String) {
        markPendingAndDispatch(taskId, cancelExistingWork = false)
    }

    override suspend fun enqueueAll(taskIds: List<String>) {
        if (taskIds.isEmpty()) return
        val now = System.currentTimeMillis()
        taskIds.forEach { taskId ->
            val task = repository.getTask(taskId) ?: return@forEach
            if (task.status in terminalStatuses) return@forEach
            repository.markPending(taskId = taskId, nextScheduleAt = now)
        }
        // 所有任务标记完成后只触发一次，避免 REPLACE 反复取消前一次 DispatchWork
        workController.enqueueDispatchWork(replace = true)
    }

    override suspend fun resume(taskId: String) {
        markPendingAndDispatch(taskId, cancelExistingWork = true)
    }

    override suspend fun pause(taskId: String) {
        val task = repository.getTask(taskId) ?: return
        if (task.status == DownloadStatus.PAUSED || task.status in terminalStatuses) return
        repository.markPaused(taskId)
        workController.cancelTaskWork(taskId)
        workController.enqueueDispatchWork(replace = true)
    }

    override suspend fun cancel(taskId: String) {
        val task = repository.getTask(taskId) ?: return
        if (task.status in terminalStatuses) return
        repository.markCanceled(taskId = taskId, message = "用户取消下载")
        workController.cancelTaskWork(taskId)
        workController.enqueueDispatchWork(replace = true)
    }

    override suspend fun cancelAndAwaitStopped(taskId: String, timeoutMs: Long): Boolean {
        val stopped = workController.cancelTaskAndAwaitStopped(taskId, timeoutMs)
        if (stopped) workController.enqueueDispatchWork(replace = true)
        return stopped
    }

    override suspend fun restorePendingTasks(limit: Int) {
        repository.resetInterruptedTasks()
        workController.enqueueDispatchWork(replace = true)
    }

    override suspend fun onNetworkAvailable() {
        repository.requeueWaitingForNetworkTasks(now = System.currentTimeMillis())
        workController.enqueueDispatchWork(replace = true)
    }

    // ── 私有帮助函数 ────────────────────────────────────────────────────────

    /**
     * 将任务置为 PENDING 并触发一次调度分发。
     *
     * @param cancelExistingWork resume 场景传 true，先停掉旧 Work 再重新入队，
     *                           避免旧 Worker 在 backoff / waiting 期间占着 token。
     */
    private suspend fun markPendingAndDispatch(taskId: String, cancelExistingWork: Boolean) {
        val task = repository.getTask(taskId) ?: return
        if (task.status in terminalStatuses) return
        repository.markPending(taskId = taskId, nextScheduleAt = System.currentTimeMillis())
        if (cancelExistingWork) workController.cancelTaskWork(taskId)
        workController.enqueueDispatchWork(replace = true)
    }
}