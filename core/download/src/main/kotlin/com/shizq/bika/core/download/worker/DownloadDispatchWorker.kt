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
            workController.enqueueDispatchWork(
                delayMs = delayMs,
                replace = true,
            )
        }

        return Result.success()
    }
}