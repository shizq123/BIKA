package com.shizq.bika.core.download.scheduler

import androidx.datastore.core.DataStore
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.preferences.DownloadPreferences
import com.shizq.bika.core.model.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [DefaultDownloadQueuePolicy] 的退避与并发上限。
 *
 * 退避曲线是纯函数，但它决定了失败任务的唤醒频率，写错的代价是后台流量与电量，
 * 线上不容易察觉，所以这里把「单调、封顶、边界不崩」逐条钉死。
 */
class DefaultDownloadQueuePolicyTest {

    // ── 退避曲线 ────────────────────────────────────────────────────────

    @Test
    fun `退避从 10 秒开始按 2 的幂增长`() {
        val policy = policyWith()

        assertEquals(10_000L, policy.nextRetryDelayMs(1))
        assertEquals(20_000L, policy.nextRetryDelayMs(2))
        assertEquals(40_000L, policy.nextRetryDelayMs(3))
        assertEquals(80_000L, policy.nextRetryDelayMs(4))
    }

    /**
     * 记录当前实现与文档意图不一致的一处缺口：
     *
     * ```
     * val delay = 10_000L * (1L shl (attempt - 1).coerceAtMost(6))
     * return min(delay, 30 * 60 * 1000L)
     * ```
     *
     * 指数在 `coerceAtMost(6)` 处被封死，`delay` 的实际上限是
     * `10_000 * 2^6 = 640_000ms`（约 10.67 分钟），比它自身之后
     * `min(delay, 30 分钟)` 里写的封顶值小得多 —— 那个 30 分钟封顶
     * 永远触发不到，是死代码。真正生效的封顶是 640 秒。
     *
     * 若设计意图确实是 30 分钟，指数上限需要改成 8（`2^8 * 10_000 =
     * 2_560_000`，配合 `min` 才能在 1_800_000 处截断）。修复后应把
     * 本测试的期望值改回 `30 * 60 * 1000L`。
     */
    @Test
    fun `已知缺口 - 退避实际封顶约 10_67 分钟而非 30 分钟`() {
        val policy = policyWith()
        val actualCap = 640_000L

        for (attempt in 7..50) {
            assertEquals(actualCap, policy.nextRetryDelayMs(attempt), "attempt=$attempt 未封顶")
        }
    }

    @Test
    fun `退避单调不减`() {
        val policy = policyWith()

        var previous = 0L
        for (attempt in 1..40) {
            val current = policy.nextRetryDelayMs(attempt)
            assertTrue(current >= previous, "attempt=$attempt 退避回退了: $previous -> $current")
            previous = current
        }
    }

    @Test
    fun `retryCount 为 0 或负数时按首次重试处理`() {
        val policy = policyWith()

        // 调用方传的是 retryCount + 1，理论上不会 <= 0；
        // 但这里一旦崩或返回负延迟，会让任务被立刻重新调度形成忙循环。
        assertEquals(10_000L, policy.nextRetryDelayMs(0))
        assertEquals(10_000L, policy.nextRetryDelayMs(-5))
        assertTrue(policy.nextRetryDelayMs(Int.MIN_VALUE) > 0L)
    }

    @Test
    fun `极大 retryCount 不发生移位溢出`() {
        val policy = policyWith()

        // (attempt - 1) 若不做 coerceAtMost，1L shl 大数会回绕成负数或 0，
        // 进而让延迟变成极小甚至负值。当前实现的实际封顶是 640_000ms
        // （见"已知缺口"用例），这里只钉住"不溢出、恒为正"这个更基础的性质，
        // 不依赖封顶具体数值，修复退避曲线时不需要连带改这条。
        val delay = policy.nextRetryDelayMs(Int.MAX_VALUE)
        assertTrue(delay > 0L, "溢出导致了非正延迟: $delay")
        assertEquals(policy.nextRetryDelayMs(7), delay, "封顶后应恒定不变")
    }

    // ── 并发上限 ────────────────────────────────────────────────────────

    // 这三个用例刻意不套 runTest：maxConcurrentChapters() 是同步方法，内部用
    // runBlocking 读 DataStore（本身就是 P1-3 报告的问题）。在 runTest 里再嵌一层
    // 阻塞调用只会把测试和被测实现的调度耦合起来，无法反映真实调用方的行为。
    @Test
    fun `并发数读取用户偏好`() {
        val policy = policyWith(maxConcurrentDownloads = 5)
        assertEquals(5, policy.maxConcurrentChapters())
    }

    @Test
    fun `并发数至少为 1`() {
        // 偏好里存了 0 或负数时若原样返回，Semaphore/claim 的槽位判断会永远不放行，
        // 表现为「下载功能整体不工作」，且用户无法自行恢复。
        assertEquals(1, policyWith(maxConcurrentDownloads = 0).maxConcurrentChapters())
        assertEquals(1, policyWith(maxConcurrentDownloads = -3).maxConcurrentChapters())
    }

    @Test
    fun `偏好读取失败时回退默认值`() {
        val policy = DefaultDownloadQueuePolicy(
            UserPreferencesDataSource(ThrowingPreferencesStore()),
        )
        assertEquals(3, policy.maxConcurrentChapters())
    }

    // ── 测试辅助 ────────────────────────────────────────────────────────

    private fun policyWith(maxConcurrentDownloads: Int = 3): DefaultDownloadQueuePolicy =
        DefaultDownloadQueuePolicy(
            UserPreferencesDataSource(
                FakePreferencesStore(
                    UserPreferences(
                        download = DownloadPreferences(
                            maxConcurrentDownloads = maxConcurrentDownloads,
                        ),
                    ),
                ),
            ),
        )

    private class FakePreferencesStore(
        initial: UserPreferences,
    ) : DataStore<UserPreferences> {
        override val data: Flow<UserPreferences>
            field = MutableStateFlow(initial)

        override suspend fun updateData(
            transform: suspend (t: UserPreferences) -> UserPreferences,
        ): UserPreferences = transform(data.value).also { data.value = it }
    }

    private class ThrowingPreferencesStore : DataStore<UserPreferences> {
        override val data: Flow<UserPreferences> = flow {
            throw IllegalStateException("DataStore 尚未就绪")
        }

        override suspend fun updateData(
            transform: suspend (t: UserPreferences) -> UserPreferences,
        ): UserPreferences = throw UnsupportedOperationException()
    }
}
