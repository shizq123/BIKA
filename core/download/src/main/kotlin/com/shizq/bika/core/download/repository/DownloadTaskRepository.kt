package com.shizq.bika.core.download.repository

import com.shizq.bika.core.database.model.DownloadErrorCode
import com.shizq.bika.core.download.model.DownloadTask
import kotlinx.coroutines.flow.Flow

interface DownloadTaskRepository {

    fun observeAllTasks(): Flow<List<DownloadTask>>

    fun observeTasksByComic(comicId: String): Flow<List<DownloadTask>>

    fun observeTask(taskId: String): Flow<DownloadTask?>

    suspend fun getTask(taskId: String): DownloadTask?

    suspend fun getSchedulableTasks(limit: Int = Int.MAX_VALUE): List<DownloadTask>

    suspend fun saveTask(task: DownloadTask)

    suspend fun markPending(taskId: String)

    suspend fun markWaitingForNetwork(
        taskId: String,
        errorCode: DownloadErrorCode,
        message: String = "",
    )

    suspend fun markDownloading(taskId: String)

    suspend fun markPaused(taskId: String)

    suspend fun markCanceled(taskId: String, message: String = "")

    suspend fun markFailed(
        taskId: String,
        errorCode: DownloadErrorCode,
        message: String,
        incrementRetryCount: Boolean = true,
    )

    suspend fun markCompleted(
        taskId: String,
        localPath: String,
        totalPages: Int,
    )

    suspend fun updateProgress(
        taskId: String,
        downloadedPages: Int,
        totalPages: Int,
    )

    suspend fun updateLocalPath(taskId: String, localPath: String)

    suspend fun markAsViewed(taskId: String)

    suspend fun bringToTop(taskId: String)

    suspend fun updatePriority(taskId: String, priority: Int)

    suspend fun deleteTask(taskId: String)

    suspend fun resetInterruptedTasks(): Int
}