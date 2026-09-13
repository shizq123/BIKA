package com.shizq.bika.feature.reader.impl.util.preload

import coil3.request.ImageRequest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 预载窗口与滚动方向判定。
 *
 * 这段逻辑的失效方式是**静默的**：方向判反或窗口算错只会让预载落到用户不会去的
 * 一侧，表现为"翻页时偶尔要等图"，不报错、日志里也看不出来。所以这里不测
 * "有没有真的下载"，而是钉住"按什么顺序请求了哪几个下标"。
 *
 * [ListPreloader] 不再自己持有 Context/ImageLoader（投递出口抽成了
 * [PreloadRequestEnqueuer]），因此可以在纯 JVM 下断言具体索引。
 *
 * 真实 [ImageRequest] 的构造需要 Android Context，测试里造不出来，
 * 所以顺序记录在 [RecordingModelProvider]：它是预载循环里每个**可用**项
 * 必经的一步，记在这里与记在投递出口等价。
 */
class ListPreloaderTest {

    @Test
    fun `首次回调按向前滚动预载后方若干项`() {
        val f = fixture(itemCount = 100, maxPreload = 3)

        // lastFirstVisibleIndex 初值为 -1，firstVisible=0 > -1 判定为向前。
        f.preloader.onScroll(firstVisible = 0, lastVisible = 4)

        assertEquals(listOf(5, 6, 7), f.requested())
    }

    @Test
    fun `继续向前滚动时从最后可见项之后开始`() {
        val f = fixture(itemCount = 100, maxPreload = 2)

        f.preloader.onScroll(firstVisible = 0, lastVisible = 4)
        f.clear()
        f.preloader.onScroll(firstVisible = 5, lastVisible = 9)

        assertEquals(listOf(10, 11), f.requested())
    }

    @Test
    fun `向后滚动时预载前方且下标递减`() {
        val f = fixture(itemCount = 100, maxPreload = 3)

        // 先向前推进，把 lastFirstVisibleIndex 抬到 20
        f.preloader.onScroll(firstVisible = 20, lastVisible = 24)
        f.clear()
        // firstVisible 回落，判定为向后
        f.preloader.onScroll(firstVisible = 15, lastVisible = 19)

        assertEquals(listOf(14, 13, 12), f.requested())
    }

    @Test
    fun `预载窗口在列表末尾处截断`() {
        val f = fixture(itemCount = 8, maxPreload = 5)

        f.preloader.onScroll(firstVisible = 3, lastVisible = 6)

        assertEquals(listOf(7), f.requested(), "只剩第 7 项，不能越界")
    }

    @Test
    fun `预载窗口在列表开头处截断`() {
        val f = fixture(itemCount = 50, maxPreload = 5)

        f.preloader.onScroll(firstVisible = 10, lastVisible = 14)
        f.clear()
        f.preloader.onScroll(firstVisible = 2, lastVisible = 6)

        assertEquals(listOf(1, 0), f.requested(), "到 0 为止，不能出现负下标")
    }

    @Test
    fun `预载数为 0 时不发起任何请求`() {
        // 用户在设置里关掉预载即为此路径，必须完全静默。
        val f = fixture(itemCount = 100, maxPreload = 0)

        f.preloader.onScroll(firstVisible = 0, lastVisible = 4)

        assertTrue(f.requested().isEmpty())
    }

    @Test
    fun `列表为空时不发起请求`() {
        val f = fixture(itemCount = 0, maxPreload = 3)

        f.preloader.onScroll(firstVisible = 0, lastVisible = 4)

        assertTrue(f.requested().isEmpty())
    }

    @Test
    fun `lastVisible 为负表示尚未布局`() {
        val f = fixture(itemCount = 100, maxPreload = 3)

        f.preloader.onScroll(firstVisible = 0, lastVisible = -1)

        assertTrue(f.requested().isEmpty())
    }

    @Test
    fun `占位项被跳过而不中断整个窗口`() {
        // 分页占位符处 getItem 返回 null。若这里 break 而非 continue，
        // 占位项之后的已加载页会全部漏掉预载。
        val f = fixture(itemCount = 100, maxPreload = 4, unavailable = setOf(6, 7))

        f.preloader.onScroll(firstVisible = 0, lastVisible = 4)

        assertEquals(listOf(5, 8), f.requested())
    }

    @Test
    fun `新建实例的方向状态从零开始`() {
        // 切章后 PagingPreload 以 scrollStateProvider 为 key 重建 preloader。
        // 若沿用旧实例，新章从第 0 页开始会被判成"向后滚动"，
        // 首屏之后的页漏掉一轮预载 —— 正是重建要修的问题。
        val recorder = RecordingModelProvider()
        val data = FakeDataProvider(itemCount = 100, unavailable = emptySet())
        val enqueuer = NoopEnqueuer()

        val stale = ListPreloader(data, recorder, enqueuer, maxPreload = 3)
        stale.onScroll(firstVisible = 40, lastVisible = 44)
        recorder.clear()

        // 沿用旧实例：firstVisible=0 < 40，被判成向后，只往前预载（第 0 页之前无内容）
        stale.onScroll(firstVisible = 0, lastVisible = 4)
        assertTrue(recorder.indices.isEmpty(), "复用旧实例确实漏掉了向前预载")

        // 重建实例：方向状态归零，正常预载后方
        recorder.clear()
        val fresh = ListPreloader(data, recorder, enqueuer, maxPreload = 3)
        fresh.onScroll(firstVisible = 0, lastVisible = 4)
        assertEquals(listOf(5, 6, 7), recorder.indices)
    }

    // ── 测试辅助 ────────────────────────────────────────────────────────

    private fun fixture(
        itemCount: Int,
        maxPreload: Int,
        unavailable: Set<Int> = emptySet(),
    ): Fixture {
        val recorder = RecordingModelProvider()
        return Fixture(
            recorder = recorder,
            preloader = ListPreloader(
                dataProvider = FakeDataProvider(itemCount, unavailable),
                modelProvider = recorder,
                enqueuer = NoopEnqueuer(),
                maxPreload = maxPreload,
            ),
        )
    }

    private class Fixture(
        private val recorder: RecordingModelProvider,
        val preloader: ListPreloader<Int>,
    ) {
        fun requested(): List<Int> = recorder.indices
        fun clear() = recorder.clear()
    }

    /** 数据项直接用下标本身，省去 item -> index 的映射。 */
    private class FakeDataProvider(
        override val itemCount: Int,
        private val unavailable: Set<Int>,
    ) : PreloadDataProvider<Int> {
        override fun getItem(index: Int): Int? =
            if (index in unavailable) null else index
    }

    /**
     * 记录预载循环问到的下标顺序。返回 null 让 [ListPreloader] 跳过实际投递，
     * 从而不需要构造真实 [ImageRequest]（那需要 Android Context）。
     */
    private class RecordingModelProvider : PreloadModelProvider<Int> {
        val indices = mutableListOf<Int>()

        override fun getPreloadRequest(item: Int): ImageRequest? {
            indices += item
            return null
        }

        fun clear() = indices.clear()
    }

    private class NoopEnqueuer : PreloadRequestEnqueuer {
        override fun enqueue(request: ImageRequest) = Unit
    }
}
