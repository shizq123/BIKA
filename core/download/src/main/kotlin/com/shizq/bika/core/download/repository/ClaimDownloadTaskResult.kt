package com.shizq.bika.core.download.repository

import com.shizq.bika.core.download.model.DownloadTask

sealed interface ClaimDownloadTaskResult {
    data class Claimed(val task: DownloadTask) : ClaimDownloadTaskResult
    data object NoSlot : ClaimDownloadTaskResult
    data object NotRunnable : ClaimDownloadTaskResult
    data object NotFound : ClaimDownloadTaskResult
}