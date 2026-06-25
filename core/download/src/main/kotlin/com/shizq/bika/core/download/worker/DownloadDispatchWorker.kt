package com.shizq.bika.core.download.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.scheduler.DownloadQueuePolicy
import com.shizq.bika.core.download.scheduler.DownloadWorkController
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.math.max

@HiltWorker
class DownloadDispatchWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: DownloadTaskRepository,
    private val workController: DownloadWorkController,
    private val queuePolicy: DownloadQueuePolicy,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val maxConcurrent = queuePolicy.maxConcurrentChapters()
        val running = repository.countRunningTasks()

        val freeSlots = max(0, maxConcurrent - running)

        if (freeSlots > 0) {
            // 故意多拿一点候选，避免部分候选已经有 active work 导致空槽闲置
            val candidates = repository.getDispatchableTasks(
                now = now,
                limit = freeSlots * 3,
            )

            candidates.forEach { task ->
                workController.enqueueTaskWork(
                    taskId = task.id,
                    replace = false,
                )
            }
        }

        val nextAt = repository.getNextPendingScheduleAt(now)
        if (nextAt != null) {
            val delayMs = (nextAt - now).coerceAtLeast(1_000L)
            // 使用 replace = false（KEEP 策略）：此时当前 dispatch worker 自身还在运行中，
            // 若使用 REPLACE 会让 WorkManager 立刻取消正在执行的自身，触发 WorkerStoppedException。
            // KEEP 策略下，若队列中已有同名 work 则保留（不重复入队）；
            // 当前这次执行正常结束后，带 delay 的下一次 dispatch 已在队列中等待。
            workController.enqueueDispatchWork(
                delayMs = delayMs,
                replace = false,
            )
        }

        return Result.success()
    }
}