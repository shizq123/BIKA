package com.shizq.bika.core.download.repository

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
    private val clock: Clock,
) : DownloadTaskRepository {

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
        val statuses = listOf(
            DownloadStatus.PENDING.name,
            DownloadStatus.WAITING_FOR_NETWORK.name,
            DownloadStatus.DOWNLOADING.name,
        )
        return downloadTaskDao.getByStatuses(
            statusNames = statuses,
            limit = limit,
        ).map { it.asExternalModel() }
    }


    override suspend fun saveTask(task: DownloadTask) {
        val now = clock.now()
        val normalized = task.copy(updatedAt = now)
        downloadTaskDao.upsert(normalized.asEntity())
    }

    override suspend fun markPending(taskId: String) {
        val now = clock.now()
        downloadTaskDao.updateStatus(
            taskId = taskId,
            status = DownloadStatus.PENDING,
            errorCode = DownloadErrorCode.NONE,
            errorMessage = "",
            retryDelta = 0,
            completedAt = null,
            updatedAt = now,
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
}