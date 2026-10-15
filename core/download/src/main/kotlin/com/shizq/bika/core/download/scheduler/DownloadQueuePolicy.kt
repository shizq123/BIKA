package com.shizq.bika.core.download.scheduler

interface DownloadQueuePolicy {
    suspend fun maxConcurrentChapters(): Int
    fun nextRetryDelayMs(retryCountAfterIncrement: Int): Long
}