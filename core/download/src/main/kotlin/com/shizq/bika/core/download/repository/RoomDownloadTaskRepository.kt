package com.shizq.bika.core.download.repository

import com.shizq.bika.core.database.dao.ClaimTaskOutcome
import com.shizq.bika.core.database.dao.DownloadTaskDao
import com.shizq.bika.core.database.model.DownloadErrorCode
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.download.model.asEntity
import com.shizq.bika.core.download.model.asExternalModel
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt
import kotlin.time.Clock

@Singleton
class RoomDownloadTaskRepository @Inject constructor(
    private val downloadTaskDao: DownloadTaskDao,
    private val clock: Clock = Clock.System
) : DownloadTaskRepository {
    companion object {
        /**
         * 可被调度器感知的"活跃"状态集合。
         * PENDING / WAITING_FOR_NETWORK 等待执行，DOWNLOADING 正在执行。
         * 新增状态时同步维护此列表。
         */
        private val SCHEDULABLE_STATUSES = listOf(
            DownloadStatus.PENDING.name,
            DownloadStatus.WAITING_FOR_NETWORK.name,
            DownloadStatus.DOWNLOADING.name,
        )
    }

    override fun observeAllTasks(): Flow<List<DownloadTask>> =
        downloadTaskDao.observeAll()
            .map { list -> list.map { it.asExternalModel() } }

    override fun observeTasksByComic(comicId: String): Flow<List<DownloadTask>> =
        downloadTaskDao.observeByComic(comicId)
            .map { list -> list.map { it.asExternalModel() } }

    override fun observeTask(taskId: String): Flow<DownloadTask?> =
        downloadTaskDao.observeById(taskId)
            .map { entity -> entity?.asExternalModel() }

    override suspend fun getTask(taskId: String): DownloadTask? =
        downloadTaskDao.getById(taskId)?.asExternalModel()

    override suspend fun getSchedulableTasks(limit: Int): List<DownloadTask> {
        return downloadTaskDao.getByStatuses(
            statusNames = SCHEDULABLE_STATUSES,
            limit = limit,
        ).map { it.asExternalModel() }
    }


    override suspend fun saveTask(task: DownloadTask) {
        val now = clock.now()
        val normalized = task.copy(updatedAt = now)
        downloadTaskDao.upsert(normalized.asEntity())
    }

    override suspend fun saveTasks(tasks: List<DownloadTask>) {
        val now = clock.now()
        val entities = tasks.map { it.copy(updatedAt = now).asEntity() }
        downloadTaskDao.upsert(entities)
    }

    override suspend fun markPending(taskId: String, nextScheduleAt: Long) {
        downloadTaskDao.markPending(
            taskId = taskId,
            nextScheduleAt = nextScheduleAt,
            now = clock.now().toEpochMilliseconds(),
        )
    }

    override suspend fun markWaitingForNetwork(
        taskId: String,
        errorCode: DownloadErrorCode,
        message: String,
    ) {
        val now = clock.now()
        downloadTaskDao.updateStatus(
            taskId = taskId,
            status = DownloadStatus.WAITING_FOR_NETWORK,
            errorCode = errorCode,
            errorMessage = message,
            retryDelta = 0,
            completedAt = null,
            updatedAt = now,
        )
    }

    override suspend fun markDownloading(taskId: String) {
        val now = clock.now()
        downloadTaskDao.updateStatus(
            taskId = taskId,
            status = DownloadStatus.DOWNLOADING,
            errorCode = DownloadErrorCode.NONE,
            errorMessage = "",
            retryDelta = 0,
            completedAt = null,
            updatedAt = now,
        )
    }

    override suspend fun markPaused(taskId: String) {
        val now = clock.now()
        downloadTaskDao.updateStatus(
            taskId = taskId,
            status = DownloadStatus.PAUSED,
            errorCode = DownloadErrorCode.NONE,
            errorMessage = "",
            retryDelta = 0,
            completedAt = null,
            updatedAt = now,
        )
    }

    override suspend fun markCanceled(taskId: String, message: String) {
        val now = clock.now()
        downloadTaskDao.updateStatus(
            taskId = taskId,
            status = DownloadStatus.CANCELED,
            errorCode = DownloadErrorCode.CANCELED,
            errorMessage = message,
            retryDelta = 0,
            completedAt = null,
            updatedAt = now,
        )
    }

    override suspend fun markFailed(
        taskId: String,
        errorCode: DownloadErrorCode,
        message: String,
        incrementRetryCount: Boolean,
    ) {
        val now = clock.now()
        downloadTaskDao.updateStatus(
            taskId = taskId,
            status = DownloadStatus.FAILED,
            errorCode = errorCode,
            errorMessage = message,
            retryDelta = if (incrementRetryCount) 1 else 0,
            completedAt = null,
            updatedAt = now,
        )
    }

    override suspend fun markCompleted(
        taskId: String,
        localPath: String,
        totalPages: Int,
    ) {
        val now = clock.now()
        downloadTaskDao.markCompleted(
            taskId = taskId,
            status = DownloadStatus.COMPLETED,
            errorCode = DownloadErrorCode.NONE,
            errorMessage = "",
            localPath = localPath,
            totalPages = totalPages,
            downloadedPages = totalPages,
            completedAt = now,
            updatedAt = now,
        )
    }

    override suspend fun updateProgress(
        taskId: String,
        downloadedPages: Int,
        totalPages: Int,
    ) {
        val safeTotalPages = totalPages.coerceAtLeast(0)
        val safeDownloadedPages = downloadedPages.coerceAtLeast(0).coerceAtMost(safeTotalPages)

        val progress = if (safeTotalPages == 0) {
            0
        } else {
            ((safeDownloadedPages.toDouble() / safeTotalPages.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
        }

        downloadTaskDao.updateProgress(
            taskId = taskId,
            downloadedPages = safeDownloadedPages,
            totalPages = safeTotalPages,
            progress = progress,
            updatedAt = clock.now(),
        )
    }

    override suspend fun updateLocalPath(taskId: String, localPath: String) {
        downloadTaskDao.updateLocalPath(
            taskId = taskId,
            localPath = localPath,
            updatedAt = clock.now(),
        )
    }

    override suspend fun markAsViewed(taskId: String) {
        downloadTaskDao.markAsViewed(
            taskId = taskId,
            updatedAt = clock.now(),
        )
    }

    override suspend fun bringToTop(taskId: String) {
        downloadTaskDao.bringToTop(
            taskId = taskId,
            updatedAt = clock.now(),
        )
    }

    override suspend fun updatePriority(taskId: String, priority: Int) {
        downloadTaskDao.updatePriority(
            taskId = taskId,
            priority = priority,
            updatedAt = clock.now(),
        )
    }

    override suspend fun deleteTask(taskId: String) {
        downloadTaskDao.deleteById(taskId)
    }

    override suspend fun resetInterruptedTasks(): Int {
        return downloadTaskDao.replaceStatus(
            sourceStatus = DownloadStatus.DOWNLOADING,
            targetStatus = DownloadStatus.PENDING,
            errorCode = DownloadErrorCode.NONE,
            errorMessage = "",
            updatedAt = clock.now(),
        )
    }

    override suspend fun tryClaimTask(
        taskId: String,
        workerToken: String,
        maxConcurrent: Int,
    ): ClaimDownloadTaskResult {
        val outcome = downloadTaskDao.claimPendingTaskTransactionally(
            taskId = taskId,
            workerToken = workerToken,
            maxConcurrent = maxConcurrent,
            now = clock.now().toEpochMilliseconds(),
        )
        return when (outcome) {
            is ClaimTaskOutcome.Claimed ->
                ClaimDownloadTaskResult.Claimed(outcome.task.asExternalModel())

            ClaimTaskOutcome.NoSlot -> ClaimDownloadTaskResult.NoSlot
            ClaimTaskOutcome.NotRunnable -> ClaimDownloadTaskResult.NotRunnable
            ClaimTaskOutcome.NotFound -> ClaimDownloadTaskResult.NotFound
        }
    }

    override suspend fun countRunningTasks(): Int =
        downloadTaskDao.countTasksByStatus(DownloadStatus.DOWNLOADING)

    override suspend fun getDispatchableTasks(
        now: Long,
        limit: Int,
    ): List<DownloadTask> =
        downloadTaskDao.getDispatchableTasks(
            now = now,
            limit = limit,
        ).map { it.asExternalModel() }

    override suspend fun getNextPendingScheduleAt(now: Long): Long? =
        downloadTaskDao.getNextPendingScheduleAt(now = now)

    override suspend fun updateProgressOwned(
        taskId: String,
        workerToken: String,
        downloadedPages: Int,
        totalPages: Int,
    ) {
        downloadTaskDao.updateProgressOwned(
            taskId = taskId,
            workerToken = workerToken,
            downloadedPages = downloadedPages,
            totalPages = totalPages,
            now = clock.now().toEpochMilliseconds(),
        )
    }

    override suspend fun markCompletedOwned(
        taskId: String,
        workerToken: String,
        localPath: String,
        totalPages: Int,
    ): Boolean =
        downloadTaskDao.markCompletedOwned(
            taskId = taskId,
            workerToken = workerToken,
            localPath = localPath,
            totalPages = totalPages,
            now = clock.now().toEpochMilliseconds(),
        ) == 1

    override suspend fun markWaitingForNetworkOwned(
        taskId: String,
        workerToken: String,
        errorCode: DownloadErrorCode,
        message: String,
    ): Boolean =
        downloadTaskDao.markWaitingForNetworkOwned(
            taskId = taskId,
            workerToken = workerToken,
            errorCode = errorCode.name,
            message = message,
            now = clock.now().toEpochMilliseconds(),
        ) == 1

    override suspend fun requeueRecoverableOwned(
        taskId: String,
        workerToken: String,
        nextScheduleAt: Long,
        errorCode: DownloadErrorCode,
        message: String,
    ): Boolean =
        downloadTaskDao.requeueRecoverableOwned(
            taskId = taskId,
            workerToken = workerToken,
            nextScheduleAt = nextScheduleAt,
            errorCode = errorCode.name,
            message = message,
            now = clock.now().toEpochMilliseconds(),
        ) == 1

    override suspend fun markFailedOwned(
        taskId: String,
        workerToken: String,
        errorCode: DownloadErrorCode,
        message: String,
        incrementRetryCount: Boolean,
    ): Boolean =
        downloadTaskDao.markFailedOwned(
            taskId = taskId,
            workerToken = workerToken,
            errorCode = errorCode.name,
            message = message,
            incrementRetryCount = incrementRetryCount,
            now = clock.now().toEpochMilliseconds(),
        ) == 1

    override suspend fun requeueWaitingForNetworkTasks(now: Long): Int =
        downloadTaskDao.requeueWaitingForNetworkTasks(now = now)

}