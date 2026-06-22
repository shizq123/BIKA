package com.shizq.bika.core.download.scheduler

interface DownloadQueuePolicy {
    fun maxConcurrentChapters(): Int
    fun nextRetryDelayMs(retryCountAfterIncrement: Int): Long
}