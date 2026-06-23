package com.shizq.bika.core.download.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shizq.bika.core.download.executor.ChapterDownloadExecutor
import com.shizq.bika.core.download.executor.ChapterDownloadResult
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.download.repository.ClaimDownloadTaskResult
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.scheduler.DownloadQueuePolicy
import com.shizq.bika.core.download.scheduler.DownloadWorkController
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class ChapterDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: DownloadTaskRepository,
    private val executor: ChapterDownloadExecutor,
    private val queuePolicy: DownloadQueuePolicy,
    private val workController: DownloadWorkController,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(DownloadWorkSpec.KEY_TASK_ID)
            ?: return Result.failure()

        val workerToken = id.toString()

        return try {
            when (
                val claimResult = repository.tryClaimTask(
                    taskId = taskId,
                    workerToken = workerToken,
                    maxConcurrent = queuePolicy.maxConcurrentChapters(),
                )
            ) {
                ClaimDownloadTaskResult.NotFound -> Result.success()

                ClaimDownloadTaskResult.NotRunnable -> Result.success()

                // NoSlot 不需要在这里额外触发 Dispatch：
                // finally 块已经保证每个 Worker 退出时都会触发一次 enqueueDispatchWork，
                // 避免多个 NoSlot Worker 并发退出时重复 replace，造成调度风暴。
                ClaimDownloadTaskResult.NoSlot -> Result.success()

                is ClaimDownloadTaskResult.Claimed -> {
                    handleClaimedTask(
                        task = claimResult.task,
                        workerToken = workerToken,
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } finally {
            workController.enqueueDispatchWork(replace = true)
        }
    }

    private suspend fun handleClaimedTask(
        task: DownloadTask,
        workerToken: String,
    ): Result {
        return when (val result = executor.execute(task, workerToken)) {
            is ChapterDownloadResult.Success -> {
                repository.markCompletedOwned(
                    taskId = task.id,
                    workerToken = workerToken,
                    localPath = result.localPath,
                    totalPages = result.totalPages,
                )
                Result.success()
            }

            is ChapterDownloadResult.WaitingForNetwork -> {
                repository.markWaitingForNetworkOwned(
                    taskId = task.id,
                    workerToken = workerToken,
                    errorCode = result.errorCode,
                    message = result.message,
                )
                Result.success()
            }

            is ChapterDownloadResult.Failed -> {
                if (result.recoverable) {
                    val nextDelay = queuePolicy.nextRetryDelayMs(task.retryCount + 1)
                    val nextScheduleAt = System.currentTimeMillis() + nextDelay

                    repository.requeueRecoverableOwned(
                        taskId = task.id,
                        workerToken = workerToken,
                        nextScheduleAt = nextScheduleAt,
                        errorCode = result.errorCode,
                        message = result.message,
                    )
                } else {
                    repository.markFailedOwned(
                        taskId = task.id,
                        workerToken = workerToken,
                        errorCode = result.errorCode,
                        message = result.message,
                        incrementRetryCount = false,
                    )
                }

                Result.success()
            }
        }
    }
}