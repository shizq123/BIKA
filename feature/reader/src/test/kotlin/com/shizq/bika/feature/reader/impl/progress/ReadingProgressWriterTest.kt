package com.shizq.bika.feature.reader.impl.progress

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * [ReadingProgressWriter] 的防抖、闸门与写入顺序。
 *
 * 旧实现这部分逻辑分散在 ReadingProgressManager 的三个可变字段
 * （persistJob / persistProgress / @Volatile lastKnownPage）和四个调用点上，
 * 没有任何测试；persistJob 被生命周期回调协程与跟踪协程同时读写。
 * 折叠成单 collector 流水线后，这些行为可以在虚拟时间里确定性验证。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReadingProgressWriterTest {

    private val debounce = 1_000.milliseconds

    @Test
    fun `闸门关闭时提交被丢弃`() = runTest {
        val sink = RecordingSink()
        val writer = writer(sink, backgroundScope)

        writer.submit(progress(page = 5))
        advanceUntilIdle()

        assertTrue(sink.writes.isEmpty(), "恢复未确认前不允许写库")
    }

    @Test
    fun `开闸后防抖窗口内只写最后一次`() = runTest {
        val sink = RecordingSink()
        val writer = writer(sink, backgroundScope)
        writer.openGate()

        writer.submit(progress(page = 1))
        advanceTimeBy(200)
        writer.submit(progress(page = 2))
        advanceTimeBy(200)
        writer.submit(progress(page = 3))
        advanceUntilIdle()

        assertEquals(listOf(3), sink.writes.map { it.pageIndex })
    }

    @Test
    fun `防抖窗口跨越后各写一次`() = runTest {
        val sink = RecordingSink()
        val writer = writer(sink, backgroundScope)
        writer.openGate()

        writer.submit(progress(page = 1))
        advanceTimeBy(debounce.inWholeMilliseconds + 100)
        writer.submit(progress(page = 7))
        advanceUntilIdle()

        assertEquals(listOf(1, 7), sink.writes.map { it.pageIndex })
    }

    @Test
    fun `flush 立即写入最近一次提交值`() = runTest {
        val sink = RecordingSink()
        val writer = writer(sink, backgroundScope)
        writer.openGate()

        writer.submit(progress(page = 42))
        // 不等防抖窗口，直接 flush（模拟 ON_STOP）
        writer.flush()
        advanceTimeBy(10)

        assertEquals(
            listOf(42),
            sink.writes.map { it.pageIndex },
            "ON_STOP 必须能在防抖触发前把当前页落库",
        )
    }

    @Test
    fun `flush 不需要调用方记住页码`() = runTest {
        // 这是删掉 @Volatile lastKnownPage 的依据：flush 自己从 replayCache 取值。
        val sink = RecordingSink()
        val writer = writer(sink, backgroundScope)
        writer.openGate()

        writer.submit(progress(page = 3))
        advanceUntilIdle()
        writer.flush()
        advanceUntilIdle()

        // 防抖已写过 3，flush 取到同一个值，被 distinctUntilChanged 去重
        assertEquals(listOf(3), sink.writes.map { it.pageIndex })
    }

    @Test
    fun `闸门关闭时 flush 不写`() = runTest {
        val sink = RecordingSink()
        val writer = writer(sink, backgroundScope)

        writer.submit(progress(page = 9))
        writer.flush()
        advanceUntilIdle()

        assertTrue(sink.writes.isEmpty())
    }

    @Test
    fun `切章写入与防抖写入共用同一个串行 collector`() = runTest {
        val sink = RecordingSink()
        val writer = writer(sink, backgroundScope)
        writer.openGate()

        writer.submit(progress(page = 18, order = 1))
        // 防抖还没触发就切章：旧章节进度直接写，新章节闸门关闭
        writer.storeImmediately(progress(page = 20, order = 1))
        writer.closeGate()
        advanceUntilIdle()

        assertEquals(
            listOf(20),
            sink.writes.map { it.pageIndex },
            "只应有切章那一条写入；页码取切章时的真实位置，不是防抖里那个较早的值",
        )
        assertEquals(listOf(1), sink.writes.map { it.chapterOrder })
    }

    @Test
    fun `切章后闸门关闭 新章节恢复确认前不写`() = runTest {
        val sink = RecordingSink()
        val writer = writer(sink, backgroundScope)
        writer.openGate()
        writer.closeGate()

        writer.submit(progress(page = 0, order = 2))
        advanceUntilIdle()

        assertTrue(
            sink.writes.isEmpty(),
            "旧实现的一次性门闩放开后无法关闭，切章后会写入恢复期间的中间位置",
        )
    }

    @Test
    fun `closeGate 清掉上一章的 replayCache`() = runTest {
        val sink = RecordingSink()
        val writer = writer(sink, backgroundScope)
        writer.openGate()
        writer.submit(progress(page = 88, order = 1))
        advanceUntilIdle()
        sink.writes.clear()

        writer.closeGate()
        writer.openGate()
        // 未提交任何新值就 flush：不应把上一章的 88 写到新章节名下
        writer.flush()
        advanceUntilIdle()

        assertTrue(sink.writes.isEmpty())
    }

    @Test
    fun `相邻重复提交只写一次`() = runTest {
        val sink = RecordingSink()
        val writer = writer(sink, backgroundScope)
        writer.openGate()

        writer.submit(progress(page = 4))
        advanceUntilIdle()
        writer.submit(progress(page = 4))
        advanceUntilIdle()

        assertEquals(1, sink.writes.size)
    }

    @Test
    fun `切章后迟到的防抖写入不覆盖切章写入`() = runTest {
        // 时序：读到第 21 页（防抖计时中）-> 用户在第 25 页点了切章
        //       -> 切章写入第 25 页 + 关闸 -> 第 21 页的防抖到期。
        //
        // 两次写入的页码必须不同，否则 distinctUntilChanged 会把后者去重掉，
        // 测试即便在没有闸门过滤的情况下也会通过——那就测不到任何东西。
        val sink = RecordingSink()
        val writer = writer(sink, backgroundScope)
        writer.openGate()

        writer.submit(progress(page = 21, order = 1))
        advanceTimeBy(300)

        writer.storeImmediately(progress(page = 25, order = 1))
        writer.closeGate()
        advanceUntilIdle()

        assertEquals(
            listOf(25),
            sink.writes.map { it.pageIndex },
            "迟到的第 21 页必须被闸门拦掉，否则它会覆盖切章写入的第 25 页",
        )
    }

    // ── 测试辅助 ────────────────────────────────────────────────────────

    /**
     * scope 必须是 backgroundScope：writer 内部用 launchIn 起了一个**永不结束**的
     * collector（它 collect 的是 SharedFlow）。若传 TestScope 本身，runTest 在测试体
     * 结束后会等这个子协程完成，直接超时报 UncompletedCoroutinesError。
     *
     * backgroundScope 与 TestScope 共享同一个 TestCoroutineScheduler，
     * 因此 advanceTimeBy / advanceUntilIdle 仍然能驱动流水线里的防抖计时。
     */
    private fun writer(sink: ChapterProgressSink, scope: CoroutineScope) =
        ReadingProgressWriter(sink = sink, scope = scope, debounce = debounce)

    private fun progress(page: Int, order: Int = 1) = ChapterProgress(
        comicId = "comic-1",
        chapterOrder = order,
        pageIndex = page,
        totalPages = 100,
        chapterTitle = "第 $order 话",
    )

    private class RecordingSink : ChapterProgressSink {
        val writes = mutableListOf<ChapterProgress>()
        override suspend fun store(progress: ChapterProgress): Boolean {
            writes += progress
            return true
        }
    }
}
