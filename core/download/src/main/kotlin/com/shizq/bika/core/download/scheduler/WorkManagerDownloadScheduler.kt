package com.shizq.bika.core.download.scheduler

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.worker.ChapterDownloadWorker
import com.shizq.bika.core.download.worker.DownloadWorkSpec
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.TimeUnit

@Singleton
class WorkManagerDownloadScheduler @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: DownloadTaskRepository,
) : DownloadScheduler {

    private val workManager = WorkManager.Companion.getInstance(context)

    override suspend fun enqueue(taskId: String) {
        val task = repository.getTask(taskId) ?: return

        if (task.status == DownloadStatus.COMPLETED ||
            task.status == DownloadStatus.CANCELED
        ) {
            return
        }

        repository.markPending(taskId)
        schedule(taskId = taskId, replace = false)
    }

    override suspend fun resume(taskId: String) {
        val task = repository.getTask(taskId) ?: return

        if (task.status == DownloadStatus.COMPLETED ||
            task.status == DownloadStatus.CANCELED
        ) {
            return
        }

        if (task.status == DownloadStatus.DOWNLOADING) {
            return
        }

        repository.markPending(taskId)

        // 用户主动恢复，应覆盖旧的 waiting/backoff work
        schedule(taskId = taskId, replace = true)
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
        workManager.cancelUniqueWork(DownloadWorkSpec.uniqueWorkName(taskId))
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
        workManager.cancelUniqueWork(DownloadWorkSpec.uniqueWorkName(taskId))
    }

    override suspend fun restorePendingTasks(limit: Int) {
        repository.resetInterruptedTasks()

        val tasks = repository.getSchedulableTasks(limit)
        tasks.forEach { task ->
            schedule(taskId = task.id, replace = false)
        }
    }

    private fun schedule(
        taskId: String,
        replace: Boolean,
    ) {
        val request = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorkSpec.KEY_TASK_ID to taskId,
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS,
            )
            .addTag(DownloadWorkSpec.TAG_CHAPTER_DOWNLOAD)
            .addTag(DownloadWorkSpec.taskTag(taskId))
            .build()

        workManager.enqueueUniqueWork(
            DownloadWorkSpec.uniqueWorkName(taskId),
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }
}