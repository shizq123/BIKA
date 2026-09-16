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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * [ReadingProgressManager]：恢复确认后才开闸写库，以及切章时立即落盘。
 *
 * 本文件是重写的——原文件早已无法编译：它的 FakePageDataSource 实现的是
 * `awaitLoaded` / `isBeyondBounds`，而 [PageDataSource] 现在声明的是
 * `isLoaded` / `awaitLoadedOrBounds`；它还读了 `manager.writer`（private）。
 * 说明 reader 的 test 源集在 PageDataSource 那次重构后就没再编译过。
 *
 * 时间推进一律用 [advanceTimeBy] 而不是 advanceUntilIdle：
 * [ReadingProgressWriter] 内部是 `submissions.sample(debounce)`，sample 会起一个
 * 周期性 ticker，只要上游还活着就永远有下一个待执行的延时任务。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReadingProgressManagerTest {

    private val config = ProgressConfig(
        dataWaitTimeout = 10_000.milliseconds,
        confirmTimeout = 2_000.milliseconds,
        confirmTolerance = 1,
        persistDebounce = 1_000.milliseconds,
    )

    @Test
    fun `恢复确认后才跟踪页码`() = runTest {
        val sink = RecordingSink()
        val manager = manager(sink)
        val controller = FakeController()

        startSession(manager, controller, targetPage = 0)
        advanceTimeBy(SettleMs)

        assertIs<RestoreOutcome.Confirmed>(manager.restoreOutcome.value)

        controller.moveTo(30)
        advanceTimeBy(DebouncePassedMs)

        assertEquals(30, sink.writes.last().pageIndex)
    }

    @Test
    fun `跨页模式下进度取起始页`() = runTest {
        // 一屏 D(30, 31)：进度记 30。末页判定用另一个端点，见 ChapterAutoAdvanceEffect。
        val sink = RecordingSink()
        val manager = manager(sink)
        val controller = FakeController()

        startSession(manager, controller, targetPage = 0)
        advanceTimeBy(SettleMs)

        controller.moveTo(ReadingPositionSnapshot(30, 31))
        advanceTimeBy(DebouncePassedMs)

        assertEquals(30, sink.writes.last().pageIndex)
    }

    @Test
    fun `恢复未确认时不写库`() = runTest {
        // 滚动调用发生了但视口没动（分组尚未建立等）。此时视口位置不代表用户意图，
        // 写进去就会覆盖数据库里真实的进度。
        val sink = RecordingSink()
        val manager = manager(sink)
        val controller = FakeController(honorScroll = false)

        startSession(manager, controller, targetPage = 50)
        // 等满 confirmTimeout
        advanceTimeBy(3_000)

        assertIs<RestoreOutcome.Unconfirmed>(manager.restoreOutcome.value)

        controller.moveTo(60)
        advanceTimeBy(DebouncePassedMs)

        assertTrue(sink.writes.isEmpty(), "未确认就不该写库，实际: ${sink.writes}")
    }

    @Test
    fun `flush 不等防抖立即写入`() = runTest {
        val sink = RecordingSink()
        val manager = manager(sink)
        val controller = FakeController()

        startSession(manager, controller, targetPage = 0)
        advanceTimeBy(SettleMs)

        controller.moveTo(30)
        advanceTimeBy(300) // 仍在防抖窗口内
        sink.writes.clear()

        manager.flush()
        advanceTimeBy(SettleMs)

        assertEquals(1, sink.writes.size, "实际: ${sink.writes}")
        assertEquals(30, sink.writes.single().pageIndex)
    }

    @Test
    fun `切章立即保存旧章进度`() = runTest {
        val sink = RecordingSink()
        val manager = manager(sink)
        val controller = FakeController()

        // 闸门只在恢复被确认后才打开，而 onChapterSwitch 只在闸门 Open 时写旧章进度。
        startSession(manager, controller, targetPage = 0)
        advanceTimeBy(SettleMs)
        sink.writes.clear()

        manager.onChapterSwitch(
            ChapterProgress(
                comicId = "comic-1",
                chapterOrder = 1,
                pageIndex = 20,
                totalPages = 100,
                chapterTitle = "第1话",
            ),
        )
        advanceTimeBy(SettleMs)

        assertTrue(
            sink.writes.any { it.chapterOrder == 1 && it.pageIndex == 20 },
            "实际: ${sink.writes}",
        )
    }

    @Test
    fun `切章后闸门关闭_新章恢复前不写库`() = runTest {
        val sink = RecordingSink()
        val manager = manager(sink)
        val controller = FakeController()

        startSession(manager, controller, targetPage = 0)
        advanceTimeBy(SettleMs)
        manager.onChapterSwitch(null)
        sink.writes.clear()

        controller.moveTo(5)
        advanceTimeBy(DebouncePassedMs)

        assertTrue(sink.writes.isEmpty(), "切章后闸门应关回去，实际: ${sink.writes}")
    }

    @Test
    fun `恢复越界快速失败不占满超时`() = runTest {
        val sink = RecordingSink()
        val manager = manager(sink)

        backgroundScope.launch {
            manager.session(
                key = ChapterKey("comic-1", 1),
                targetPage = 18,
                totalPagesProvider = { 15 },
                chapterTitleProvider = { "第1话" },
                dataSource = FakeDataSource(loadedUpTo = 15, totalPages = 15, isComplete = true),
                controller = FakeController(),
            )
        }

        // 远小于 dataWaitTimeout(10s)
        advanceTimeBy(2_000)

        assertIs<RestoreOutcome.Unconfirmed>(manager.restoreOutcome.value)
    }

    // ── 测试辅助 ────────────────────────────────────────────────────────

    private fun TestScope.startSession(
        manager: ReadingProgressManager,
        controller: ReaderController,
        targetPage: Int,
        loadedUpTo: Int = 100,
    ) {
        backgroundScope.launch {
            manager.session(
                key = ChapterKey("comic-1", 1),
                targetPage = targetPage,
                totalPagesProvider = { 100 },
                chapterTitleProvider = { "第1话" },
                dataSource = FakeDataSource(loadedUpTo = loadedUpTo),
                controller = controller,
            )
        }
    }

    private fun TestScope.manager(sink: ChapterProgressSink) = ReadingProgressManager(
        writer = ReadingProgressWriter(
            sink = sink,
            scope = backgroundScope,
            debounce = config.persistDebounce,
        ),
        restoreStrategy = AwaitDataRestoreStrategy(),
        config = config,
    )

    private class RecordingSink : ChapterProgressSink {
        val writes = mutableListOf<ChapterProgress>()

        override suspend fun store(progress: ChapterProgress): Boolean {
            writes += progress
            return true
        }
    }

    private class FakeDataSource(
        private val loadedUpTo: Int,
        private val totalPages: Int = 500,
        private val isComplete: Boolean = false,
    ) : PageDataSource {

        override fun isLoaded(index: Int): Boolean = index in 0 until loadedUpTo

        override suspend fun awaitLoadedOrBounds(index: Int): PageLoadResult = when {
            index < 0 -> PageLoadResult.OutOfBounds(totalPages)
            index < loadedUpTo -> PageLoadResult.Loaded
            isComplete || index >= totalPages -> PageLoadResult.OutOfBounds(totalPages)
            // 既没到位也没越界：真实实现会一直挂着，等调用方的 dataWaitTimeout 兜底。
            else -> awaitCancellation()
        }
    }

    private class FakeController(
        initialPage: Int = 0,
        private val honorScroll: Boolean = true,
    ) : ReaderController {
        private var current by mutableStateOf(ReadingPositionSnapshot.single(initialPage))

        override val position: ReadingPositionSnapshot get() = current
        override val continuousScroller: ContinuousScroller? = null

        override suspend fun track() = awaitCancellation()

        fun moveTo(pageIndex: Int) = moveTo(ReadingPositionSnapshot.single(pageIndex))

        fun moveTo(snapshot: ReadingPositionSnapshot) {
            // position 是快照状态，positionFlow 基于 snapshotFlow，
            // 需要提交快照才会触发全局写观察者。
            Snapshot.withMutableSnapshot { current = snapshot }
        }

        override suspend fun scrollNextPage() = Unit
        override suspend fun scrollPrevPage() = Unit

        override suspend fun scrollToPage(index: Int) {
            if (honorScroll) moveTo(index)
        }
    }

    private companion object {
        /** 够让恢复流程与一次立即写入跑完，又不触发下一个防抖窗口。 */
        const val SettleMs = 100L

        /** 越过一个完整的 persistDebounce 窗口。 */
        const val DebouncePassedMs = 1_500L
    }
}
