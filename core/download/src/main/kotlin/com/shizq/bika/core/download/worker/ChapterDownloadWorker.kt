package com.shizq.bika.core.download.worker

import android.content.Context
import android.util.Log
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
        Log.d(TAG, "doWork start: taskId=$taskId workerToken=$workerToken")

        return try {
            when (
                val claimResult = repository.tryClaimTask(
                    taskId = taskId,
                    workerToken = workerToken,
                    maxConcurrent = queuePolicy.maxConcurrentChapters(),
                )
            ) {
                ClaimDownloadTaskResult.NotFound -> {
                    Log.w(TAG, "claim NotFound: taskId=$taskId")
                    Result.success()
                }

                ClaimDownloadTaskResult.NotRunnable -> {
                    Log.d(TAG, "claim NotRunnable: taskId=$taskId")
                    Result.success()
                }

                // NoSlot 不需要在这里额外触发 Dispatch：
                // finally 块已经保证每个 Worker 退出时都会触发一次 enqueueDispatchWork，
                // 避免多个 NoSlot Worker 并发退出时重复 replace，造成调度风暴。
                ClaimDownloadTaskResult.NoSlot -> {
                    Log.d(TAG, "claim NoSlot: taskId=$taskId")
                    Result.success()
                }

                is ClaimDownloadTaskResult.Claimed -> {
                    Log.d(TAG, "claim success: taskId=$taskId, executing...")
                    handleClaimedTask(
                        task = claimResult.task,
                        workerToken = workerToken,
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } finally {
            // 使用 KEEP 而非 REPLACE：多个 ChapterDownloadWorker 并发完成时，
            // 每个都用 replace=true 会让后一个取消前一个刚入队的 dispatch work，
            // 最终可能导致调度链断裂。只需保证队列里有一个 dispatch work 即可，
            // 不需要反复抢占。外部主动触发（enqueue/resume/cancel）仍可用 replace=true。
            workController.enqueueDispatchWork(replace = false)
        }
    }

    private suspend fun handleClaimedTask(
        task: DownloadTask,
        workerToken: String,
    ): Result {
        return when (val result = executor.execute(task, workerToken)) {
            is ChapterDownloadResult.Success -> {
                Log.d(TAG, "download success: taskId=${task.id} pages=${result.totalPages}")
                repository.markCompletedOwned(
                    taskId = task.id,
                    workerToken = workerToken,
                    localPath = result.localPath,
                    totalPages = result.totalPages,
                )
                Result.success()
            }

            is ChapterDownloadResult.WaitingForNetwork -> {
                Log.d(TAG, "waiting for network: taskId=${task.id} code=${result.errorCode}")
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
                    Log.w(
                        TAG,
                        "download failed (recoverable), retry in ${nextDelay}ms: taskId=${task.id} code=${result.errorCode}"
                    )
                    repository.requeueRecoverableOwned(
                        taskId = task.id,
                        workerToken = workerToken,
                        nextScheduleAt = nextScheduleAt,
                        errorCode = result.errorCode,
                        message = result.message,
                    )
                } else {
                    Log.e(
                        TAG,
                        "download failed (fatal): taskId=${task.id} code=${result.errorCode} msg=${result.message}"
                    )
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

    companion object {
        private const val TAG = "ChapterDownloadWorker"
    }
}