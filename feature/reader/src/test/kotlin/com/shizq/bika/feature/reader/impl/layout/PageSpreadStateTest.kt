package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.runtime.snapshots.Snapshot
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [PageSpreadState] 的宽页累积与「分组重排后回到原页」的重定位请求。
 *
 * 服务端不返回图片尺寸，宽页只能在解码后才知道，因此分组一定会在运行中变化。
 * 变化本身不可避免，危险的是变化发生在**当前位置之前**：此时
 * `pagerState.currentPage`（翻页单位下标）与真实页码的对应关系整体偏移一位，
 * 用户会看到画面无故跳到邻页。[PageSpreadState.pendingAnchorPage] 就是为这种
 * 情况留下"该回到哪一页"的线索，由渲染层消费。
 *
 * 这些属性用了 Compose 的 mutableStateOf / mutableStateMapOf，读写需要在快照
 * 上下文里进行，故统一包一层 [Snapshot.withMutableSnapshot]。
 */
class PageSpreadStateTest {

    @Test
    fun `未测量时按无宽页分组`() {
        val state = doublePageState(pageCount = 4)

        withSnapshot {
            assertEquals(2, state.spreadCount)
            assertEquals(PageSpread.Double(0, 1), state.spreads[0])
        }
    }

    @Test
    fun `测出宽页后分组重算`() {
        val state = doublePageState(pageCount = 4)

        withSnapshot {
            state.onPageMeasured(pageIndex = 1, width = 2000f, height = 1000f)

            assertEquals(3, state.spreadCount, "宽页独占一屏后翻页单位应增加")
            assertEquals(PageSpread.Single(0), state.spreads[0])
            assertEquals(PageSpread.Single(1), state.spreads[1])
            assertEquals(PageSpread.Double(2, 3), state.spreads[2])
        }
    }

    @Test
    fun `普通尺寸不改变分组`() {
        val state = doublePageState(pageCount = 4)

        withSnapshot {
            state.onPageMeasured(pageIndex = 1, width = 800f, height = 1200f)

            assertEquals(2, state.spreadCount)
            assertNull(state.pendingAnchorPage)
        }
    }

    @Test
    fun `当前位置之后的宽页不触发重定位`() {
        // 只影响用户还没看到的分组，当前屏不会变，重定位反而会造成多余的一次滚动。
        val state = doublePageState(pageCount = 10)

        withSnapshot {
            state.onPageMeasured(pageIndex = 8, width = 2000f, height = 1000f, anchorPage = 2)

            assertNull(state.pendingAnchorPage)
        }
    }

    @Test
    fun `当前位置之前的宽页请求重定位到原页`() {
        val state = doublePageState(pageCount = 10)

        withSnapshot {
            state.onPageMeasured(pageIndex = 1, width = 2000f, height = 1000f, anchorPage = 6)

            assertEquals(6, state.pendingAnchorPage, "应记下重排前用户所在的真实页码")
        }
    }

    @Test
    fun `当前页自身是宽页时也请求重定位`() {
        // 当前页从"与邻页共屏"变成"独占一屏"，自身所在的单位下标同样可能变。
        val state = doublePageState(pageCount = 10)

        withSnapshot {
            state.onPageMeasured(pageIndex = 4, width = 2000f, height = 1000f, anchorPage = 4)

            assertEquals(4, state.pendingAnchorPage)
        }
    }

    @Test
    fun `未提供 anchorPage 时不请求重定位`() {
        // 条漫模式（WebtoonLayout）不传 anchorPage：它没有翻页单位的概念。
        val state = doublePageState(pageCount = 10)

        withSnapshot {
            state.onPageMeasured(pageIndex = 1, width = 2000f, height = 1000f)

            assertNull(state.pendingAnchorPage)
        }
    }

    @Test
    fun `重复上报同一宽页只请求一次重定位`() {
        // onSizeLoaded 会随重组多次触发，若每次都记，重定位会反复打断用户滚动。
        val state = doublePageState(pageCount = 10)

        withSnapshot {
            state.onPageMeasured(pageIndex = 1, width = 2000f, height = 1000f, anchorPage = 6)
            assertEquals(6, state.consumePendingAnchor())

            state.onPageMeasured(pageIndex = 1, width = 2000f, height = 1000f, anchorPage = 6)
            assertNull(state.pendingAnchorPage, "同一页第二次上报应被短路")
        }
    }

    @Test
    fun `consumePendingAnchor 取出后清空`() {
        val state = doublePageState(pageCount = 10)

        withSnapshot {
            state.onPageMeasured(pageIndex = 0, width = 2000f, height = 1000f, anchorPage = 5)

            assertEquals(5, state.consumePendingAnchor())
            assertNull(state.consumePendingAnchor(), "第二次取应为 null，避免重复滚动")
        }
    }

    @Test
    fun `单页模式测出宽页不产生重定位`() {
        // 单页模式每屏一页，分组恒等于页码，宽页不影响任何换算。
        val state = PageSpreadState(doublePage = false, pageCountProvider = { 6 })

        withSnapshot {
            state.onPageMeasured(pageIndex = 2, width = 2000f, height = 1000f, anchorPage = 4)

            assertEquals(6, state.spreadCount)
            assertNull(state.pendingAnchorPage)
        }
    }

    @Test
    fun `页数增长时分组自动扩展`() {
        // 分页续拉会让 itemCount 增长，spreads 是 derived 的，应当无需手动同步。
        var pageCount = 4
        val state = PageSpreadState(doublePage = true, pageCountProvider = { pageCount })

        withSnapshot { assertEquals(2, state.spreadCount) }
        pageCount = 8
        withSnapshot { assertEquals(4, state.spreadCount) }
    }

    // ── 测试辅助 ────────────────────────────────────────────────────────

    private fun doublePageState(pageCount: Int) =
        PageSpreadState(doublePage = true, pageCountProvider = { pageCount })

    private fun withSnapshot(block: () -> Unit) = Snapshot.withMutableSnapshot(block)
}
