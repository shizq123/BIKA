package com.shizq.bika.feature.reader.impl.progress

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingProgressManagerTest {
    private val debounce = 1000.milliseconds
    private lateinit var testScope: TestScope
    private lateinit var sink: RecordingSink
    private lateinit var config: ProgressConfig

    @Before
    fun setup() {
        val dispatcher = StandardTestDispatcher()
        testScope = TestScope(dispatcher)
        sink = RecordingSink()
        config = ProgressConfig(
            dataWaitTimeout = 10000.milliseconds,
            confirmTimeout = 2000.milliseconds,
            confirmTolerance = 1,
            persistDebounce = 1000.milliseconds,
        )
    }

    @Test
    fun `切章时正确保存旧章进度并重置闸门`() = testScope.runTest {
        val dataSource = FakePageDataSource()
        val restoreStrategy = AwaitDataRestoreStrategy()
        val manager = manager(restoreStrategy)

        // 模拟第1章
        val chapter1Key = ChapterKey("comic-1", 1)
        dataSource.setAvailable(50)

        // 启动第1章会话并恢复到起始页
        val job1 = backgroundScope.launch {
            manager.session(
                key = chapter1Key,
                targetPage = 0,
                totalPagesProvider = { 100 },
                chapterTitleProvider = { "第1话" },
                dataSource = dataSource,
                controller = FakeReaderController(),
            )
        }
        advanceUntilIdle()

        // 验证恢复成功并开闸
        assertTrue(manager.restoreOutcome.value is RestoreOutcome.Confirmed)

        // 模拟用户翻到第20页
        val progress20 = ChapterProgress("comic-1", 1, 20, 100, "第1话")
        manager.writer.submit(progress20)
        advanceTimeBy(500) // 防抖中

        // 切到第2章
        job1.cancel()
        val oldProgress = ChapterProgress("comic-1", 1, 20, 100, "第1话")
        manager.onChapterSwitch(oldProgress)
        advanceUntilIdle()

        // 验证第1章的进度被立即保存
        assertTrue(sink.writes.any { it.chapterOrder == 1 && it.pageIndex == 20 })

        // 启动第2章会话
        val chapter2Key = ChapterKey("comic-1", 2)
        dataSource.setAvailable(80)
        val controller2 = FakeReaderController()

        backgroundScope.launch {
            manager.session(
                key = chapter2Key,
                targetPage = 0,
                totalPagesProvider = { 80 },
                chapterTitleProvider = { "第2话" },
                dataSource = dataSource,
                controller = controller2,
            )
        }
        advanceUntilIdle()

        // 第2章恢复后可以写入
        controller2.scrollTo(15)
        advanceUntilIdle()

        assertTrue(sink.writes.any { it.chapterOrder == 2 && it.pageIndex == 15 })
    }

    @Test
    fun `flush 使用 latestProgress 不依赖 replayCache`() = testScope.runTest {
        val dataSource = FakePageDataSource()
        val restoreStrategy = AwaitDataRestoreStrategy()
        val manager = manager(restoreStrategy)

        val chapterKey = ChapterKey("comic-1", 1)
        dataSource.setAvailable(50)
        val controller = FakeReaderController()

        // 启动会话
        val job = backgroundScope.launch {
            manager.session(
                key = chapterKey,
                targetPage = 0,
                totalPagesProvider = { 100 },
                chapterTitleProvider = { "第1话" },
                dataSource = dataSource,
                controller = controller,
            )
        }
        advanceUntilIdle()

        // 跟踪到第30页
        controller.scrollTo(30)
        advanceTimeBy(500) // 防抖中

        sink.writes.clear()

        // 立即 flush：应写入第30页，而不是等防抖
        manager.flush()
        advanceUntilIdle()

        assertEquals(1, sink.writes.size)
        assertEquals(30, sink.writes.last().pageIndex)

        job.cancel()
    }

    @Test
    fun `恢复越界时快速失败不卡10秒`() = testScope.runTest {
        val dataSource = FakePageDataSource()
        val restoreStrategy = AwaitDataRestoreStrategy()
        val manager = manager(restoreStrategy)

        val chapterKey = ChapterKey("comic-1", 1)

        // 章节只有15页，但尝试恢复到第18页
        dataSource.setAvailable(15)
        val controller = FakeReaderController()

        backgroundScope.launch {
            manager.session(
                key = chapterKey,
                targetPage = 18, // 越界
                totalPagesProvider = { 15 },
                chapterTitleProvider = { "第1话" },
                dataSource = dataSource,
                controller = controller,
            )
        }

        advanceTimeBy(2000) // 等待越界检测（应该很快失败）

        // 验证恢复失败，不需要等满10秒
        val state = manager.restoreOutcome.value
        assertTrue(
            state is RestoreOutcome.Unconfirmed,
            "越界恢复应快速失败，实际状态: $state",
        )
    }

    // ── 测试辅助 ────────────────────────────────────────────────────────

    private fun manager(restoreStrategy: ProgressRestoreStrategy) = ReadingProgressManager(
        writer = ReadingProgressWriter(
            sink = sink,
            scope = testScope.backgroundScope,
            debounce = config.persistDebounce,
        ),
        restoreStrategy = restoreStrategy,
        config = config,
    )

    private class RecordingSink : ChapterProgressSink {
        val writes = mutableListOf<ChapterProgress>()
        val stored = mutableMapOf<String, ChapterProgress>()

        override suspend fun store(progress: ChapterProgress): Boolean {
            writes += progress
            stored["${progress.comicId}:${progress.chapterOrder}"] = progress
            return true
        }

        suspend fun load(comicId: String, chapterOrder: Int): ChapterProgress? {
            return stored["$comicId:$chapterOrder"]
        }
    }

    private class FakePageDataSource : PageDataSource {
        private var availablePages = 0

        fun setAvailable(count: Int) {
            availablePages = count
        }

        override suspend fun awaitLoaded(pageIndex: Int): Boolean {
            return pageIndex < availablePages
        }

        override fun isBeyondBounds(pageIndex: Int, totalPages: Int): Boolean {
            return pageIndex >= totalPages
        }
    }

    private class FakeReaderController :
        com.shizq.bika.feature.reader.impl.layout.ReaderController {
        private val _visibleItemIndex = MutableStateFlow(0)
        override val visibleItemIndex: StateFlow<Int> = _visibleItemIndex
        override val continuousScroller: com.shizq.bika.feature.reader.impl.layout.ContinuousScroller? =
            null

        fun scrollTo(pageIndex: Int) {
            _visibleItemIndex.value = pageIndex
        }

        override suspend fun scrollToPage(index: Int) {
            scrollTo(index)
        }

        override suspend fun scrollNextPage() {
            _visibleItemIndex.value++
        }

        override suspend fun scrollPrevPage() {
            _visibleItemIndex.value--
        }
    }
}
