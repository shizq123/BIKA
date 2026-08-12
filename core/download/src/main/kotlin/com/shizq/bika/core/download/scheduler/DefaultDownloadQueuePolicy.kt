package com.shizq.bika.core.download.scheduler

import com.shizq.bika.core.datastore.UserPreferencesDataSource
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.min

@Singleton
class DefaultDownloadQueuePolicy @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
) : DownloadQueuePolicy {

    override fun maxConcurrentChapters(): Int {
        // 读取用户在设置页配置的"最大并发下载数"。
        // 该方法由 WorkManager 调度线程低频调用，同步读一次偏好可接受；
        // 读取失败（DataStore 尚未就绪等）时回退默认值，不让下载调度中断。
        return try {
            runBlocking {
                userPreferencesDataSource.userData.first().download.maxConcurrentDownloads
            }.coerceAtLeast(1)
        } catch (_: Exception) {
            DEFAULT_MAX_CONCURRENT_CHAPTERS
        }
    }

    override fun nextRetryDelayMs(retryCountAfterIncrement: Int): Long {
        val attempt = retryCountAfterIncrement.coerceAtLeast(1)
        val delay = 10_000L * (1L shl (attempt - 1).coerceAtMost(6))
        return min(delay, 30 * 60 * 1000L)
    }

    private companion object {
        const val DEFAULT_MAX_CONCURRENT_CHAPTERS = 3
    }
}
