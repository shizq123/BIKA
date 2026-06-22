package com.shizq.bika.core.download.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.download.executor.ChapterDownloadExecutor
import com.shizq.bika.core.download.executor.ChapterDownloadResult
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class ChapterDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: DownloadTaskRepository,
    private val executor: ChapterDownloadExecutor,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(DownloadWorkSpec.KEY_TASK_ID)
            ?: return Result.failure()

        val task = repository.getTask(taskId)
            ?: return Result.success()

        if (task.status == DownloadStatus.COMPLETED ||
            task.status == DownloadStatus.CANCELED ||
            task.status == DownloadStatus.PAUSED
        ) {
            return Result.success()
        }

        return try {
            when (val result = executor.execute(task)) {
                is ChapterDownloadResult.Success -> {
                    if (shouldSkipTerminalWrite(taskId)) {
                        return Result.success()
                    }

                    repository.markCompleted(
                        taskId = taskId,
                        localPath = result.localPath,
                        totalPages = result.totalPages,
                    )
                    Result.success()
                }

                is ChapterDownloadResult.WaitingForNetwork -> {
                    if (shouldSkipTerminalWrite(taskId)) {
                        return Result.success()
                    }

                    repository.markWaitingForNetwork(
                        taskId = taskId,
                        errorCode = result.errorCode,
                        message = result.message,
                    )
                    Result.retry()
                }

                is ChapterDownloadResult.Failed -> {
                    if (shouldSkipTerminalWrite(taskId)) {
                        return Result.success()
                    }

                    repository.markFailed(
                        taskId = taskId,
                        errorCode = result.errorCode,
                        message = result.message,
                        incrementRetryCount = result.recoverable,
                    )

                    if (result.recoverable) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        }
    }

    private suspend fun shouldSkipTerminalWrite(taskId: String): Boolean {
        val latest = repository.getTask(taskId) ?: return true
        return latest.status == DownloadStatus.PAUSED ||
                latest.status == DownloadStatus.CANCELED
    }
}