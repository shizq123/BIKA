package com.shizq.bika.core.download.domain

import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.scheduler.DownloadScheduler
import jakarta.inject.Inject

class MoveDownloadTaskToFrontUseCase @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val scheduler: DownloadScheduler,
) {
    suspend operator fun invoke(taskId: String) {
        // bringToTop 在单个事务内原子完成 getMaxPriority + updatePriority，
        // 避免并发调用时两个任务读到相同的 maxPriority 导致优先级相同。
        repository.bringToTop(taskId)
        scheduler.resume(taskId)
    }
}