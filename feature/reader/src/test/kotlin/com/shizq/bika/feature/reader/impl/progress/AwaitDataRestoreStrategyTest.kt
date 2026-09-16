package com.shizq.bika.feature.reader.impl.progress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import com.shizq.bika.feature.reader.impl.layout.ContinuousScroller
import com.shizq.bika.feature.reader.impl.layout.ReaderController
import com.shizq.bika.feature.reader.impl.layout.ReadingPositionSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
        // 本项目 PagingConfig.enablePlaceholders = true，itemCount 首屏即等于服务端
        // total（500），但真实加载的只有前 10 项。旧判据 `itemCount > targetPage`
        // 在这里为真，会立刻"成功"并放开写库闸门；正确判据 peek(400) != null 仍为假。
        val controller = FakeController(initialPage = 0)
        val dataSource = FakeDataSource(loadedUpTo = 10)

        val outcome = strategy.restore(400, dataSource, controller, config)

        assertIs<RestoreOutcome.Unconfirmed>(outcome)
        assertEquals("目标页数据加载超时", outcome.reason)
        assertTrue(controller.scrollCalls.isEmpty(), "数据没到位就不该滚")
    }

    @Test
    fun `越界时快速失败`() = runTest {
        // 数据库记录用户读到第 50 页，但章节实际只有 30 页（服务端删减了内容）。
        // 旧实现会等满 10 秒超时；新实现检测到 endOfPaginationReached 后快速返回。
        val controller = FakeController(initialPage = 0)
        val dataSource = FakeDataSource(loadedUpTo = 30, totalPages = 30, isComplete = true)

        val outcome = strategy.restore(50, dataSource, controller, config)

        assertIs<RestoreOutcome.Unconfirmed>(outcome)
        assertEquals(50, outcome.targetPage)
        assertEquals(0, outcome.reachedPage)
        assertTrue(
            outcome.reason.contains("超出章节范围"),
            "实际原因: ${outcome.reason}"
        )
        assertTrue(
            outcome.reason.contains("30"),
            "应提示实际总页数，实际原因: ${outcome.reason}"
        )
        assertTrue(controller.scrollCalls.isEmpty(), "越界不应尝试滚动")
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

    @Test
    fun `越界快速失败不等超时`() = runTest {
        // 章节实际只有 15 页，但数据库记录用户读到第 18 页。
        // 应该快速识别越界（通过 loadState），而不是等 10 秒超时。
        val controller = FakeController(initialPage = 0)
        val dataSource = FakeDataSource(loadedUpTo = 15, totalPages = 15)

        val outcome = strategy.restore(18, dataSource, controller, config)

        assertIs<RestoreOutcome.Unconfirmed>(outcome)
        assertTrue(outcome.reason.contains("超出章节范围"))
        assertTrue(outcome.reason.contains("15"))
        assertTrue(controller.scrollCalls.isEmpty(), "越界时不应尝试滚动")
    }

    @Test
    fun `越界时 reason 包含实际页数`() = runTest {
        val controller = FakeController(initialPage = 0)
        val dataSource = FakeDataSource(loadedUpTo = 20, totalPages = 20)

        val outcome = strategy.restore(25, dataSource, controller, config)

        assertIs<RestoreOutcome.Unconfirmed>(outcome)
        assertEquals(25, outcome.targetPage)
        assertTrue(outcome.reason.contains("20"), "错误信息应包含实际页数")
    }

    // ── 测试替身 ────────────────────────────────────────────────────────

    /**
     * @param loadedUpTo 前 N 项是真实数据（peek 非 null）。
     * @param totalPages 章节总页数（模拟 itemCount）。
     * @param isComplete 是否已加载完成（模拟 endOfPaginationReached）。
     *
     * 注意这个假实现**没有** itemCount 的概念——接口里也没有。真实的
     * LazyPagingItems 在 enablePlaceholders=true 下 itemCount 会立刻等于服务端
     * total，与已加载数无关；把它排除在接口外，就不可能再写出依赖它的判据。
     */
    private class FakeDataSource(
        loadedUpTo: Int,
        private val totalPages: Int = 500,
        private val isComplete: Boolean = false,
    ) : PageDataSource {
        private val loaded = MutableStateFlow(loadedUpTo)

        fun setLoadedUpTo(value: Int) {
            loaded.value = value
        }

        override fun isLoaded(index: Int): Boolean = index < loaded.value

        override suspend fun awaitLoadedOrBounds(index: Int): PageLoadResult {
            return loaded.first { currentLoaded ->
                when {
                    index < 0 -> true
                    index < currentLoaded -> true
                    isComplete && index >= totalPages -> true
                    else -> false
                }
            }.let {
                when {
                    index < 0 -> PageLoadResult.OutOfBounds(totalPages)
                    index < loaded.value -> PageLoadResult.Loaded
                    else -> PageLoadResult.OutOfBounds(totalPages)
                }
            }
        }
    }

    /**
     * position 是 Compose 快照状态。测试里直接用 mutableStateOf，读写包在
     * withMutableSnapshot 里；[positionFlow] 基于 snapshotFlow，需要
     * Snapshot 的全局写观察者才会发射，故 [scrollToPage] 里显式提交快照。
     */
    private class FakeController(
        initialPage: Int = 0,
        private val honorScroll: Boolean = true,
        private val scrollLandsAt: (Int) -> Int = { it },
    ) : ReaderController {
        private var current by mutableStateOf(ReadingPositionSnapshot.single(initialPage))
        val scrollCalls = mutableListOf<Int>()

        override val position: ReadingPositionSnapshot get() = current
        override val continuousScroller: ContinuousScroller? = null

        override suspend fun track() = awaitCancellation()

        override suspend fun scrollNextPage() = Unit
        override suspend fun scrollPrevPage() = Unit

        override suspend fun scrollToPage(index: Int) {
            scrollCalls += index
            if (honorScroll) {
                Snapshot.withMutableSnapshot {
                    current = ReadingPositionSnapshot.single(scrollLandsAt(index))
                }
            }
        }
    }
}
