package com.shizq.bika.core.download.scheduler

import androidx.datastore.core.DataStore
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.preferences.DownloadPreferences
import com.shizq.bika.core.model.preferences.UserPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [DefaultDownloadQueuePolicy] 对抗性与极端压力测试套件。
 * 覆盖：协程取消透传、高并发读取、DataStore各类异常降级、极端异常数值输入。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DefaultDownloadQueuePolicyAdversarialTest {

    // ── 1. 协程取消协同性对抗检验 (Cancellation & Timeout) ─────────────────

    @Test
    fun `协程在读取前已被取消时直接抛出 CancellationException 绝不返回降级默认值`() = runTest {
        val policy = createPolicy(3)
        var caughtException: Throwable? = null
        val job = launch {
            cancel()
            try {
                policy.maxConcurrentChapters()
            } catch (e: Throwable) {
                caughtException = e
                throw e
            }
        }
        job.join()
        assertTrue(job.isCancelled)
        assertTrue(caughtException is CancellationException, "在已取消协程中调用必须透传 CancellationException")
    }

    @Test
    fun `外部 withTimeout 超时触发时严禁吞掉超时异常`() = runTest {
        val deferred = CompletableDeferred<UserPreferences>()
        val hangingStore = object : DataStore<UserPreferences> {
            override val data: Flow<UserPreferences> = flow {
                emit(deferred.await())
            }
            override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
                throw UnsupportedOperationException()
        }
        val policy = DefaultDownloadQueuePolicy(UserPreferencesDataSource(hangingStore))

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(50) {
                policy.maxConcurrentChapters()
            }
        }
    }

    @Test
    fun `挂起等待 DataStore 期间被外部 cancelAndJoin 能够快速响应取消`() = runTest {
        val deferred = CompletableDeferred<UserPreferences>()
        val hangingStore = object : DataStore<UserPreferences> {
            override val data: Flow<UserPreferences> = flow {
                emit(deferred.await())
            }
            override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
                throw UnsupportedOperationException()
        }
        val policy = DefaultDownloadQueuePolicy(UserPreferencesDataSource(hangingStore))

        var wasCancelled = false
        val job = launch {
            try {
                policy.maxConcurrentChapters()
            } catch (e: CancellationException) {
                wasCancelled = true
                throw e
            }
        }

        testScheduler.advanceTimeBy(10)
        job.cancelAndJoin()
        assertTrue(wasCancelled, "挂起任务必须能捕获并协同 CancellationException")
    }

    @Test
    fun `父协程取消时多个并发子任务均能正确被取消而不发生悬挂`() = runTest {
        val deferred = CompletableDeferred<UserPreferences>()
        val hangingStore = object : DataStore<UserPreferences> {
            override val data: Flow<UserPreferences> = flow {
                emit(deferred.await())
            }
            override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
                throw UnsupportedOperationException()
        }
        val policy = DefaultDownloadQueuePolicy(UserPreferencesDataSource(hangingStore))

        var cancelledCount = 0
        val parentJob = launch {
            repeat(10) {
                launch {
                    try {
                        policy.maxConcurrentChapters()
                    } catch (e: CancellationException) {
                        cancelledCount++
                        throw e
                    }
                }
            }
        }

        testScheduler.advanceTimeBy(10)
        parentJob.cancelAndJoin()
        assertEquals(10, cancelledCount, "所有 10 个子协程都应当响应父协程的取消")
    }

    // ── 2. 高频并发读取对抗检验 (High Concurrency Stress) ─────────────────

    @Test
    fun `1000 个协程高频并发读取 maxConcurrentChapters 无死锁且结果一致`() = runTest {
        val policy = createPolicy(5)

        val deferredList = (1..1000).map {
            async(Dispatchers.Default) {
                policy.maxConcurrentChapters()
            }
        }

        val results = deferredList.awaitAll()
        assertEquals(1000, results.size)
        assertTrue(results.all { it == 5 }, "1000 次并发读取结果必须全部为 5")
    }

    @Test
    fun `并发动态修改偏好流并高频读取时无死锁且安全获取最新值`() = runTest {
        val stateFlow = MutableStateFlow(createPreferences(1))
        val store = object : DataStore<UserPreferences> {
            override val data: Flow<UserPreferences> = stateFlow
            override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences {
                val updated = transform(stateFlow.value)
                stateFlow.value = updated
                return updated
            }
        }
        val policy = DefaultDownloadQueuePolicy(UserPreferencesDataSource(store))

        // 启动写入协程
        val writer = launch(Dispatchers.Default) {
            val validOptions = listOf(1, 2, 3, 5)
            repeat(200) { i ->
                stateFlow.value = createPreferences(validOptions[i % validOptions.size])
            }
        }

        // 启动 500 个并发读取协程
        val readers = (1..500).map {
            async(Dispatchers.Default) {
                policy.maxConcurrentChapters()
            }
        }

        val results = readers.awaitAll()
        writer.join()

        assertEquals(500, results.size)
        assertTrue(results.all { it in listOf(1, 2, 3, 5) }, "所有读取结果必须是有效的合法并发数")
    }

    // ── 3. 异常输入与极端数值鲁棒性对抗检验 (DataStore & Value Anomalies) ──

    @Test
    fun `DataStore 抛出 IOException 时降级回退默认值 3`() = runTest {
        val throwingStore = object : DataStore<UserPreferences> {
            override val data: Flow<UserPreferences> = flow {
                throw IOException("Disk I/O error while reading preferences")
            }
            override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
                throw UnsupportedOperationException()
        }
        val policy = DefaultDownloadQueuePolicy(UserPreferencesDataSource(throwingStore))
        assertEquals(3, policy.maxConcurrentChapters())
    }

    @Test
    fun `DataStore 流为空流时 (first 抛出 NoSuchElementException) 降级回退默认值 3`() = runTest {
        val emptyStore = object : DataStore<UserPreferences> {
            override val data: Flow<UserPreferences> = emptyFlow()
            override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
                throw UnsupportedOperationException()
        }
        val policy = DefaultDownloadQueuePolicy(UserPreferencesDataSource(emptyStore))
        assertEquals(3, policy.maxConcurrentChapters())
    }

    @Test
    fun `DataStore 抛出 RuntimeException 时降级回退默认值 3`() = runTest {
        val throwingStore = object : DataStore<UserPreferences> {
            override val data: Flow<UserPreferences> = flow {
                throw RuntimeException("Corrupted proto or deserialization failure")
            }
            override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
                throw UnsupportedOperationException()
        }
        val policy = DefaultDownloadQueuePolicy(UserPreferencesDataSource(throwingStore))
        assertEquals(3, policy.maxConcurrentChapters())
    }

    @Test
    fun `偏好值为 0 或极端负数时下限防护返回 1`() = runTest {
        assertEquals(1, createPolicy(0).maxConcurrentChapters())
        assertEquals(1, createPolicy(-1).maxConcurrentChapters())
        assertEquals(1, createPolicy(-999).maxConcurrentChapters())
        assertEquals(1, createPolicy(Int.MIN_VALUE).maxConcurrentChapters())
    }

    @Test
    fun `偏好值为超大数时的表现检验`() = runTest {
        // 当前实现为 coerceAtLeast(1)，如果偏好中直接被写入大数，返回大数值
        assertEquals(10, createPolicy(10).maxConcurrentChapters())
        assertEquals(100, createPolicy(100).maxConcurrentChapters())
        assertEquals(Int.MAX_VALUE, createPolicy(Int.MAX_VALUE).maxConcurrentChapters())
    }

    // ── 辅助方法 ────────────────────────────────────────────────────────

    private fun createPreferences(maxConcurrent: Int): UserPreferences =
        UserPreferences(download = DownloadPreferences(maxConcurrentDownloads = maxConcurrent))

    private fun createPolicy(maxConcurrent: Int): DefaultDownloadQueuePolicy {
        val store = object : DataStore<UserPreferences> {
            override val data: Flow<UserPreferences> = MutableStateFlow(createPreferences(maxConcurrent))
            override suspend fun updateData(transform: suspend (UserPreferences) -> UserPreferences): UserPreferences =
                throw UnsupportedOperationException()
        }
        return DefaultDownloadQueuePolicy(UserPreferencesDataSource(store))
    }
}
