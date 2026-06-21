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
abstract class DownloadTaskDao {

    @Upsert
    abstract suspend fun upsert(task: DownloadTaskEntity)

    @Upsert
    abstract suspend fun upsert(tasks: List<DownloadTaskEntity>)

    @Delete
    abstract suspend fun delete(task: DownloadTaskEntity)

    @Delete
    abstract suspend fun delete(tasks: List<DownloadTaskEntity>)

    @Query("DELETE FROM downloadTask WHERE id = :taskId")
    abstract suspend fun deleteById(taskId: String)

    @Query("SELECT * FROM downloadTask ORDER BY priority DESC, createdAt DESC")
    abstract fun observeAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM downloadTask WHERE comicId = :comicId ORDER BY episodeOrder ASC")
    abstract fun observeByComic(comicId: String): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM downloadTask WHERE id = :taskId LIMIT 1")
    abstract fun observeById(taskId: String): Flow<DownloadTaskEntity?>

    @Query("SELECT * FROM downloadTask WHERE id = :taskId LIMIT 1")
    abstract suspend fun getById(taskId: String): DownloadTaskEntity?

    @Query(
        """
        SELECT * FROM downloadTask
        WHERE status IN (:statusNames)
        ORDER BY priority DESC, createdAt ASC
        LIMIT :limit
        """
    )
    abstract suspend fun getByStatuses(
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
    abstract suspend fun updateProgress(
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
    abstract suspend fun updateStatus(
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
    abstract suspend fun markCompleted(
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
    abstract suspend fun updateLocalPath(
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
    abstract suspend fun markAsViewed(
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
    abstract suspend fun updatePriority(
        taskId: String,
        priority: Int,
        updatedAt: Instant,
    )

    @Query("SELECT IFNULL(MAX(priority), 0) FROM downloadTask")
    abstract suspend fun getMaxPriority(): Int

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
    abstract suspend fun replaceStatus(
        sourceStatus: DownloadStatus,
        targetStatus: DownloadStatus,
        errorCode: DownloadErrorCode,
        errorMessage: String,
        updatedAt: Instant,
    ): Int

    @Transaction
    open suspend fun bringToTop(taskId: String, updatedAt: Instant) {
        val nextPriority = getMaxPriority() + 1
        updatePriority(taskId, nextPriority, updatedAt)
    }
}