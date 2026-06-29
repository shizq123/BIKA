package com.shizq.bika.core.download.model

import com.shizq.bika.core.database.model.DownloadErrorCode
import com.shizq.bika.core.database.model.DownloadStatus
import kotlin.time.Instant

data class DownloadTask(
    val id: String,
    val comicId: String,
    val comicTitle: String,
    val coverUrl: String,
    val episodeId: String,
    val episodeTitle: String,
    val episodeOrder: Int,

    val status: DownloadStatus = DownloadStatus.PENDING,

    val progress: Int = 0,
    val totalPages: Int = 0,
    val downloadedPages: Int = 0,

    val localPath: String = "",
    val isViewed: Boolean = false,
    val priority: Int = 0,

    val errorCode: DownloadErrorCode = DownloadErrorCode.NONE,
    val errorMessage: String = "",
    val retryCount: Int = 0,

    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null,
) {
    val isCompleted: Boolean
        get() = status == DownloadStatus.COMPLETED

    val canResume: Boolean
        get() = status == DownloadStatus.PAUSED ||
                status == DownloadStatus.FAILED ||
                status == DownloadStatus.WAITING_FOR_NETWORK ||
                status == DownloadStatus.PENDING

    val canDelete: Boolean
        get() = true
}