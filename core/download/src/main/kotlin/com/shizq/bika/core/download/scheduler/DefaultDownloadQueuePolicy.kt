package com.shizq.bika.core.download.scheduler

import com.shizq.bika.core.datastore.UserPreferencesDataSource
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlin.math.min

@Singleton
class DefaultDownloadQueuePolicy @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
) : DownloadQueuePolicy {

    override suspend fun maxConcurrentChapters(): Int {
        // 读取用户在设置页配置的"最大并发下载数"。
        // 挂起读取偏好流的首个快照，避免 runBlocking 阻塞后台 Worker 线程；
        // 读取失败（DataStore 尚未就绪等）时回退默认值，不让下载调度中断。
        return try {
            userPreferencesDataSource.userData.first().download.maxConcurrentDownloads.coerceAtLeast(1)
        } catch (e: CancellationException) {
            throw e
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
