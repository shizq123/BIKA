package com.shizq.bika.core.download.domain

sealed interface DeleteDownloadTaskResult {

    data object Deleted : DeleteDownloadTaskResult

    data object NotFound : DeleteDownloadTaskResult

    data class Failed(
        val reason: Reason,
        val message: String,
    ) : DeleteDownloadTaskResult {
        enum class Reason {
            CANCEL_TIMEOUT,
            FILE_DELETE_FAILED,
            DATABASE_DELETE_FAILED,
        }
    }
}