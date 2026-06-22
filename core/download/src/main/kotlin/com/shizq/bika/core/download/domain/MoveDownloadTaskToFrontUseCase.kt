package com.shizq.bika.core.download.domain

import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.scheduler.DownloadScheduler
import jakarta.inject.Inject

class MoveDownloadTaskToFrontUseCase @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val scheduler: DownloadScheduler,
) {
    suspend operator fun invoke(taskId: String) {
        val maxPriority = repository.getMaxPriority()
        repository.setPriority(taskId, maxPriority + 1)
        scheduler.resume(taskId)
    }
}