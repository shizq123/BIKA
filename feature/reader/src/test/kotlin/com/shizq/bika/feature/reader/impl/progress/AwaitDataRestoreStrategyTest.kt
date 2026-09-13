package com.shizq.bika.feature.reader.impl.progress

import com.shizq.bika.feature.reader.impl.layout.ContinuousScroller
import com.shizq.bika.feature.reader.impl.layout.ReaderController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * [AwaitDataRestoreStrategy]：等数据 → 滚一次 → 确认到达。
 *
 * 重点覆盖旧 RetryRestoreStrategy 的两个结构缺陷：
 * 1. 就绪判据用 itemCount，在 enablePlaceholders=true 下恒真（见 `占位项不算数据到位`）
 * 2. 超时后降级成 Restored(fallbackPage) 并放开写库闸门（见 `未确认时不返回 Confirmed`）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AwaitDataRestoreStrategyTest {

    private val strategy = AwaitDataRestoreStrategy()
    private val config = ProgressConfig()

    @Test
    fun `目标页为 0 直接确认`() = runTest {
        val controller = FakeController()
        val outcome = strategy.restore(0, FakeDataSource(loadedUpTo = 0), controller, config)

        assertEquals(RestoreOutcome.Confirmed(0), outcome)
        assertTrue(controller.scrollCalls.isEmpty(), "第 0 页无需滚动")
    }

    @Test
    fun `已在目标页时不重复滚动`() = runTest {
        val controller = FakeController(initialPage = 30)
        val outcome = strategy.restore(30, FakeDataSource(loadedUpTo = 100), controller, config)

        assertEquals(RestoreOutcome.Confirmed(30), outcome)
        assertTrue(
            controller.scrollCalls.isEmpty(),
            "rememberLazyListState 的 initial 值已生效时应省掉多余滚动",
        )
    }

    @Test
    fun `数据到位后滚一次并确认`() = runTest {
        val controller = FakeController(initialPage = 0)
        val dataSource = FakeDataSource(loadedUpTo = 100)

        val outcome = strategy.restore(50, dataSource, controller, config)

        assertEquals(RestoreOutcome.Confirmed(50), outcome)
        assertEquals(
            listOf(50),
            controller.scrollCalls,
            "只滚一次，不是旧实现那样每 100ms 重试最多 150 次",
        )
    }

    @Test
    fun `占位项不算数据到位`() = runTest {
        // 本项目 PagingConfig.enablePlaceholders = true，itemCount 首屏即等于服务端 total。
        // 旧判据 `itemCount > targetPage` 在这里为真，会立刻"成功"；
        // 正确判据 peek(index) != null 仍为假，必须继续等。
        val controller = FakeController(initialPage = 0)
        val dataSource = FakeDataSource(
            loadedUpTo = 10,       // 只有前 10 项是真实数据
            declaredCount = 500,   // 但 itemCount 报了 500（含 placeholder）
        )

        val outcome = strategy.restore(400, dataSource, controller, config)

        assertIs<RestoreOutcome.Unconfirmed>(outcome)
        assertEquals("目标页数据加载超时", outcome.reason)
        assertTrue(controller.scrollCalls.isEmpty(), "数据没到位就不该滚")
    }

    @Test
    fun `数据延迟到位仍能确认`() = runTest {
        val controller = FakeController(initialPage = 0)
        val dataSource = FakeDataSource(loadedUpTo = 0)

        // 在 dataWaitTimeout 内让数据到位。用 backgroundScope 而非裸 launch：
        // runTest 会等所有子协程结束，裸 launch 一个立即完成的协程也可以，
        // 但 backgroundScope 语义更明确——它是"环境"而非被测行为的一部分。
        backgroundScope.launch { dataSource.setLoadedUpTo(100) }

        val outcome = strategy.restore(50, dataSource, controller, config)

        assertEquals(RestoreOutcome.Confirmed(50), outcome)
    }

    @Test
    fun `未确认时不返回 Confirmed`() = runTest {
        // 模拟 PagerController.scrollToPage 在 spreads 为空时静默返回：
        // 滚动调用发生了，但视口没动。旧实现会降级成 Restored(fallbackPage) 并开闸，
        // 于是 1 秒后把这个错误位置写进数据库，覆盖真实进度。
        val controller = FakeController(initialPage = 0, honorScroll = false)
        val dataSource = FakeDataSource(loadedUpTo = 100)

        val outcome = strategy.restore(50, dataSource, controller, config)

        assertIs<RestoreOutcome.Unconfirmed>(outcome)
        assertEquals(50, outcome.targetPage)
        assertEquals(0, outcome.reachedPage)
        assertEquals("滚动后未确认到达目标页", outcome.reason)
    }

    @Test
    fun `容许 tolerance 内的误差`() = runTest {
        // 条漫下图片高度未测完时 firstVisibleItemIndex 可能差一项
        val controller = FakeController(initialPage = 0, scrollLandsAt = { it - 1 })
        val dataSource = FakeDataSource(loadedUpTo = 100)

        val outcome = strategy.restore(50, dataSource, controller, config)

        assertIs<RestoreOutcome.Confirmed>(outcome)
        assertEquals(49, outcome.page)
    }

    @Test
    fun `超出 tolerance 视为未确认`() = runTest {
        val controller = FakeController(initialPage = 0, scrollLandsAt = { it - 10 })
        val dataSource = FakeDataSource(loadedUpTo = 100)

        val outcome = strategy.restore(50, dataSource, controller, config)

        assertIs<RestoreOutcome.Unconfirmed>(outcome)
    }

    // ── 测试替身 ────────────────────────────────────────────────────────

    /**
     * @param loadedUpTo 前 N 项是真实数据（peek 非 null）
     * @param reportedCount itemCount 报告值，默认等于 loadedUpTo。
     *   传入更大的值即模拟 enablePlaceholders=true：itemCount 远大于真实已加载数。
     */
    private class FakeDataSource(
        loadedUpTo: Int,
        private val declaredCount: Int? = null,
    ) : PageDataSource {
        private val loaded = MutableStateFlow(loadedUpTo)

        fun setLoadedUpTo(value: Int) {
            loaded.value = value
        }

        override fun isLoaded(index: Int): Boolean = index < loaded.value

        override val reportedCount: Int get() = declaredCount ?: loaded.value

        override suspend fun awaitLoaded(index: Int) {
            loaded.first { index < it }
        }
    }

    private class FakeController(
        initialPage: Int = 0,
        private val honorScroll: Boolean = true,
        private val scrollLandsAt: (Int) -> Int = { it },
    ) : ReaderController {
        private val page = MutableStateFlow(initialPage)
        val scrollCalls = mutableListOf<Int>()

        override val visibleItemIndex: Flow<Int> = page
        override val continuousScroller: ContinuousScroller? = null

        override suspend fun scrollNextPage() = Unit
        override suspend fun scrollPrevPage() = Unit

        override suspend fun scrollToPage(index: Int) {
            scrollCalls += index
            if (honorScroll) page.value = scrollLandsAt(index)
        }
    }
}
