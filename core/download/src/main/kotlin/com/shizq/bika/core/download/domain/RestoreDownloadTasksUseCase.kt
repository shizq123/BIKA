package com.shizq.bika.core.download.domain

import com.shizq.bika.core.download.scheduler.DownloadScheduler
import jakarta.inject.Inject

class RestoreDownloadTasksUseCase @Inject constructor(
    private val scheduler: DownloadScheduler,
) {
    suspend operator fun invoke() {
        scheduler.restorePendingTasks()
    }
}