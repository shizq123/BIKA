package com.shizq.bika.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.shizq.bika.core.database.model.DownloadErrorCode
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.database.model.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

@Dao
interface DownloadTaskDao {

    @Upsert
    suspend fun upsert(task: DownloadTaskEntity)

    @Upsert
    suspend fun upsert(tasks: List<DownloadTaskEntity>)

    @Delete
    suspend fun delete(task: DownloadTaskEntity)

    @Delete
    suspend fun delete(tasks: List<DownloadTaskEntity>)

    @Query("DELETE FROM downloadTask WHERE id = :taskId")
    suspend fun deleteById(taskId: String)

    @Query("SELECT * FROM downloadTask ORDER BY priority DESC, createdAt DESC")
    fun observeAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM downloadTask WHERE comicId = :comicId ORDER BY episodeOrder ASC")
    fun observeByComic(comicId: String): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM downloadTask WHERE id = :taskId LIMIT 1")
    fun observeById(taskId: String): Flow<DownloadTaskEntity?>

    @Query("SELECT * FROM downloadTask WHERE id = :taskId LIMIT 1")
    suspend fun getById(taskId: String): DownloadTaskEntity?

    @Query(
        """
        SELECT * FROM downloadTask
        WHERE status IN (:statusNames)
        ORDER BY priority DESC, createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun getByStatuses(
        statusNames: List<String>,
        limit: Int,
    ): List<DownloadTaskEntity>

    @Query(
        """
        UPDATE downloadTask
        SET downloadedPages = :downloadedPages,
            totalPages = :totalPages,
            progress = :progress,
            updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun updateProgress(
        taskId: String,
        downloadedPages: Int,
        totalPages: Int,
        progress: Int,
        updatedAt: Instant,
    )

    @Query(
        """
        UPDATE downloadTask
        SET status = :status,
            errorCode = :errorCode,
            errorMessage = :errorMessage,
            retryCount = retryCount + :retryDelta,
            completedAt = :completedAt,
            updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun updateStatus(
        taskId: String,
        status: DownloadStatus,
        errorCode: DownloadErrorCode,
        errorMessage: String,
        retryDelta: Int,
        completedAt: Instant?,
        updatedAt: Instant,
    )

    @Query(
        """
        UPDATE downloadTask
        SET status = :status,
            errorCode = :errorCode,
            errorMessage = :errorMessage,
            localPath = :localPath,
            progress = 100,
            totalPages = :totalPages,
            downloadedPages = :downloadedPages,
            completedAt = :completedAt,
            updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun markCompleted(
        taskId: String,
        status: DownloadStatus,
        errorCode: DownloadErrorCode,
        errorMessage: String,
        localPath: String,
        totalPages: Int,
        downloadedPages: Int,
        completedAt: Instant,
        updatedAt: Instant,
    )

    @Query(
        """
        UPDATE downloadTask
        SET localPath = :localPath,
            updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun updateLocalPath(
        taskId: String,
        localPath: String,
        updatedAt: Instant,
    )

    @Query(
        """
        UPDATE downloadTask
        SET isViewed = 1,
            updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun markAsViewed(
        taskId: String,
        updatedAt: Instant,
    )

    @Query(
        """
        UPDATE downloadTask
        SET priority = :priority,
            updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun updatePriority(
        taskId: String,
        priority: Int,
        updatedAt: Instant,
    )

    @Query(
        """
        UPDATE downloadTask
        SET status = :targetStatus,
            errorCode = :errorCode,
            errorMessage = :errorMessage,
            updatedAt = :updatedAt
        WHERE status = :sourceStatus
        """
    )
    suspend fun replaceStatus(
        sourceStatus: DownloadStatus,
        targetStatus: DownloadStatus,
        errorCode: DownloadErrorCode,
        errorMessage: String,
        updatedAt: Instant,
    ): Int

    @Transaction
    suspend fun bringToTop(taskId: String, updatedAt: Instant) {
        val nextPriority = getMaxPriority() + 1
        updatePriority(taskId, nextPriority, updatedAt)
    }

    @Query(
        """
    SELECT * FROM downloadTask
    WHERE status = :status
      AND next_schedule_at <= :now
    ORDER BY priority DESC, createdAt ASC, id ASC
    LIMIT :limit
"""
    )
    suspend fun getDispatchableTasks(
        status: DownloadStatus = DownloadStatus.PENDING,
        now: Long,
        limit: Int,
    ): List<DownloadTaskEntity>

    @Query(
        """
    SELECT MIN(next_schedule_at)
    FROM downloadTask
    WHERE status = :status
      AND next_schedule_at > :now
"""
    )
    suspend fun getNextPendingScheduleAt(
        status: DownloadStatus = DownloadStatus.PENDING,
        now: Long,
    ): Long?

    @Query(
        """
    SELECT COUNT(*)
    FROM downloadTask
    WHERE status = :status
"""
    )
    suspend fun countTasksByStatus(
        status: DownloadStatus,
    ): Int

    @Query(
        """
    SELECT * FROM downloadTask
    WHERE id = :taskId
    LIMIT 1
"""
    )
    suspend fun getTaskEntityById(taskId: String): DownloadTaskEntity?

    @Query(
        """
    UPDATE downloadTask
    SET status = :downloadingStatus,
        worker_token = :workerToken,
        updatedAt = :now,
        errorCode = NULL,
        errorMessage = NULL
    WHERE id = :taskId
      AND status = :pendingStatus
      AND next_schedule_at <= :now
"""
    )
    suspend fun claimPendingTask(
        taskId: String,
        workerToken: String,
        now: Long,
        pendingStatus: DownloadStatus = DownloadStatus.PENDING,
        downloadingStatus: DownloadStatus = DownloadStatus.DOWNLOADING,
    ): Int

    @Query(
        """
    UPDATE downloadTask
    SET downloadedPages = :downloadedPages,
        totalPages = :totalPages,
        updatedAt = :now
    WHERE id = :taskId
      AND worker_token = :workerToken
"""
    )
    suspend fun updateProgressOwned(
        taskId: String,
        workerToken: String,
        downloadedPages: Int,
        totalPages: Int,
        now: Long,
    ): Int

    @Query(
        """
    UPDATE downloadTask
    SET status = :completedStatus,
        worker_token = NULL,
        localPath = :localPath,
        downloadedPages = :totalPages,
        totalPages = :totalPages,
        errorCode = NULL,
        errorMessage = NULL,
        updatedAt = :now,
        completedAt = :now
    WHERE id = :taskId
      AND worker_token = :workerToken
"""
    )
    suspend fun markCompletedOwned(
        taskId: String,
        workerToken: String,
        localPath: String,
        totalPages: Int,
        now: Long,
        completedStatus: DownloadStatus = DownloadStatus.COMPLETED,
    ): Int

    @Query(
        """
    UPDATE downloadTask
    SET status = :waitingStatus,
        worker_token = NULL,
        errorCode = :errorCode,
        errorMessage = :message,
        updatedAt = :now
    WHERE id = :taskId
      AND worker_token = :workerToken
"""
    )
    suspend fun markWaitingForNetworkOwned(
        taskId: String,
        workerToken: String,
        errorCode: String?,
        message: String?,
        now: Long,
        waitingStatus: DownloadStatus = DownloadStatus.WAITING_FOR_NETWORK,
    ): Int

    @Query(
        """
    UPDATE downloadTask
    SET status = :pendingStatus,
        worker_token = NULL,
        retryCount = retryCount + 1,
        next_schedule_at = :nextScheduleAt,
        errorCode = :errorCode,
        errorMessage = :message,
        updatedAt = :now
    WHERE id = :taskId
      AND worker_token = :workerToken
"""
    )
    suspend fun requeueRecoverableOwned(
        taskId: String,
        workerToken: String,
        nextScheduleAt: Long,
        errorCode: String?,
        message: String?,
        now: Long,
        pendingStatus: DownloadStatus = DownloadStatus.PENDING,
    ): Int

    @Query(
        """
    UPDATE downloadTask
    SET status = :failedStatus,
        worker_token = NULL,
        retryCount = CASE WHEN :incrementRetryCount THEN retryCount + 1 ELSE retryCount END,
        errorCode = :errorCode,
        errorMessage = :message,
        updatedAt = :now
    WHERE id = :taskId
      AND worker_token = :workerToken
"""
    )
    suspend fun markFailedOwned(
        taskId: String,
        workerToken: String,
        errorCode: String?,
        message: String?,
        incrementRetryCount: Boolean,
        now: Long,
        failedStatus: DownloadStatus = DownloadStatus.FAILED,
    ): Int

    @Query(
        """
    UPDATE downloadTask
    SET status = :pendingStatus,
        next_schedule_at = :now,
        updatedAt = :now,
        errorCode = NULL,
        errorMessage = NULL
    WHERE status = :waitingStatus
"""
    )
    suspend fun requeueWaitingForNetworkTasks(
        now: Long,
        waitingStatus: DownloadStatus = DownloadStatus.WAITING_FOR_NETWORK,
        pendingStatus: DownloadStatus = DownloadStatus.PENDING,
    ): Int

    @Query(
        """
    SELECT COALESCE(MAX(priority), 0)
    FROM downloadTask
"""
    )
    suspend fun getMaxPriority(): Int

    @Query(
        """
    UPDATE downloadTask
    SET status = :pendingStatus,
        worker_token = NULL,
        next_schedule_at = :nextScheduleAt,
        updatedAt = :nextScheduleAt,
        errorCode = NULL,
        errorMessage = NULL
    WHERE id = :taskId
"""
    )
    suspend fun markPending(
        taskId: String,
        nextScheduleAt: Long,
        pendingStatus: DownloadStatus = DownloadStatus.PENDING,
    ): Int

    /**
     * 在单个事务内原子地认领一个待下载任务。
     *
     * 校验顺序：任务存在 -> 处于 PENDING -> 已到调度时间 -> 仍有空闲并发槽位，
     * 全部满足后写入 worker_token 并切换到 DOWNLOADING，避免多 Worker 竞态。
     */
    @Transaction
    suspend fun claimPendingTaskTransactionally(
        taskId: String,
        workerToken: String,
        maxConcurrent: Int,
        now: Long,
    ): ClaimTaskOutcome {
        val entity = getTaskEntityById(taskId)
            ?: return ClaimTaskOutcome.NotFound

        if (entity.status != DownloadStatus.PENDING) {
            return ClaimTaskOutcome.NotRunnable
        }

        if (entity.nextScheduleAt > now) {
            return ClaimTaskOutcome.NotRunnable
        }

        val runningCount = countTasksByStatus(DownloadStatus.DOWNLOADING)
        if (runningCount >= maxConcurrent) {
            return ClaimTaskOutcome.NoSlot
        }

        val updated = claimPendingTask(
            taskId = taskId,
            workerToken = workerToken,
            now = now,
        )

        if (updated != 1) {
            return ClaimTaskOutcome.NotRunnable
        }

        val claimed = getTaskEntityById(taskId)
            ?: return ClaimTaskOutcome.NotFound

        return ClaimTaskOutcome.Claimed(claimed)
    }
}

/** [DownloadTaskDao.claimPendingTaskTransactionally] 的认领结果。 */
sealed interface ClaimTaskOutcome {
    data class Claimed(val task: DownloadTaskEntity) : ClaimTaskOutcome
    data object NoSlot : ClaimTaskOutcome
    data object NotRunnable : ClaimTaskOutcome
    data object NotFound : ClaimTaskOutcome
}