package com.shizq.bika.core.download.scheduler

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.min

@Singleton
class DefaultDownloadQueuePolicy @Inject constructor() : DownloadQueuePolicy {

    override fun maxConcurrentChapters(): Int = 2

    override fun nextRetryDelayMs(retryCountAfterIncrement: Int): Long {
        val attempt = retryCountAfterIncrement.coerceAtLeast(1)
        val delay = 10_000L * (1L shl (attempt - 1).coerceAtMost(6))
        return min(delay, 30 * 60 * 1000L)
    }
}