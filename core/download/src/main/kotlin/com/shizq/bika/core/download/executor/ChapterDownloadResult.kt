package com.shizq.bika.core.download.executor

import com.shizq.bika.core.database.model.DownloadErrorCode

sealed interface ChapterDownloadResult {

    data class Success(
        val localPath: String,
        val totalPages: Int,
    ) : ChapterDownloadResult

    data class WaitingForNetwork(
        val errorCode: DownloadErrorCode,
        val message: String,
    ) : ChapterDownloadResult

    data class Failed(
        val errorCode: DownloadErrorCode,
        val message: String,
        val recoverable: Boolean,
    ) : ChapterDownloadResult
}