package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.runtime.snapshots.Snapshot
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * [PageSpreadState]：宽页累积 + 「分组重排后回到原页」的重定位请求。
 *
 * 服务端不返回图片尺寸，宽页只能在解码后才知道，因此分组一定会在运行中变化。
 * 变化本身不可避免，危险的是变化让**当前屏**对应的真实页码整体偏移——用户会看到
 * 画面无故跳到邻页。[PageSpreadState.relocateTo] 就是留给渲染层的「该回到哪一页」。
 *
 * 判定逻辑本体在纯函数 [withMeasurement] 里（见 SpreadLayoutTest），这里只覆盖
 * 状态外壳的行为：幂等、不覆盖未完成的请求、以及**非破坏性**读取。
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
            val before = state.layout
            state.onPageMeasured(pageIndex = 1, width = 800f, height = 1200f)

            assertSame(before, state.layout, "无变化时不应写入新的 layout，避免多余重组")
            assertNull(state.relocateTo)
        }
    }

    @Test
    fun `当前位置之后的宽页不触发重定位`() {
        // 只影响用户还没看到的分组，当前屏不变，重定位反而是一次多余的滚动。
        val state = doublePageState(pageCount = 10)

        withSnapshot {
            state.onPageMeasured(pageIndex = 8, width = 2000f, height = 1000f, anchorPage = 2)

            assertNull(state.relocateTo)
        }
    }

    @Test
    fun `当前位置之前的宽页请求重定位到原页`() {
        val state = doublePageState(pageCount = 10)

        withSnapshot {
            state.onPageMeasured(pageIndex = 1, width = 2000f, height = 1000f, anchorPage = 6)

            assertEquals(6, state.relocateTo, "应记下重排前用户所在的真实页码")
        }
    }

    @Test
    fun `未提供 anchorPage 时不请求重定位`() {
        // 条漫模式（WebtoonLayout）不传 anchorPage：它没有翻页单位的概念。
        val state = doublePageState(pageCount = 10)

        withSnapshot {
            state.onPageMeasured(pageIndex = 1, width = 2000f, height = 1000f)

            assertNull(state.relocateTo)
        }
    }

    @Test
    fun `重复上报同一宽页只请求一次重定位`() {
        // onSizeLoaded 会随重组多次触发，若每次都记，重定位会反复打断用户滚动。
        val state = doublePageState(pageCount = 10)

        withSnapshot {
            state.onPageMeasured(pageIndex = 1, width = 2000f, height = 1000f, anchorPage = 6)
            assertEquals(6, state.relocateTo)
            state.clearRelocation()

            state.onPageMeasured(pageIndex = 1, width = 2000f, height = 1000f, anchorPage = 6)
            assertNull(state.relocateTo, "同一页第二次上报应被短路")
        }
    }

    @Test
    fun `读取重定位请求不清除它`() {
        // 这是与旧 consumePendingAnchor() 的关键差别。旧实现先清空再滚动，而滚动前
        // 还有一个「分组是否就绪」的守卫，守卫为假时请求已经没了、滚动也没做，
        // 重定位永久失效——偏偏那个守卫在最需要重定位的那一帧最可能为假。
        val state = doublePageState(pageCount = 10)

        withSnapshot {
            state.onPageMeasured(pageIndex = 0, width = 2000f, height = 1000f, anchorPage = 5)

            assertEquals(5, state.relocateTo)
            assertEquals(5, state.relocateTo, "重复读应仍在，只有确认到位才清")

            state.clearRelocation()
            assertNull(state.relocateTo)
        }
    }

    @Test
    fun `未完成的重定位请求不被后到的覆盖`() {
        // 同一帧内多页并发上报时，先到的那个 anchor 才是用户真正的位置；
        // 后到的是在已经被改过的分组上算出来的。
        val state = doublePageState(pageCount = 20)

        withSnapshot {
            state.onPageMeasured(pageIndex = 1, width = 2000f, height = 1000f, anchorPage = 10)
            assertEquals(10, state.relocateTo)

            state.onPageMeasured(pageIndex = 3, width = 2000f, height = 1000f, anchorPage = 11)
            assertEquals(10, state.relocateTo, "先到的 anchor 优先")
        }
    }

    @Test
    fun `单页模式测出宽页不产生重定位`() {
        // 单页模式每屏一页，分组恒等于页码，宽页不影响任何换算。
        val state = PageSpreadState(doublePage = false, pageCountProvider = { 6 })

        withSnapshot {
            state.onPageMeasured(pageIndex = 2, width = 2000f, height = 1000f, anchorPage = 4)

            assertEquals(6, state.spreadCount)
            assertNull(state.relocateTo)
        }
    }

    @Test
    fun `syncPageCount 跟进分页续拉`() {
        var pageCount = 4
        val state = PageSpreadState(doublePage = true, pageCountProvider = { pageCount })

        withSnapshot { assertEquals(2, state.spreadCount) }
        pageCount = 8
        withSnapshot {
            state.syncPageCount()
            assertEquals(4, state.spreadCount)
        }
    }

    @Test
    fun `syncPageCount 不产生重定位请求`() {
        // 页数增长只在末尾追加分组，已有分组的边界不变，当前屏不受影响。
        var pageCount = 4
        val state = PageSpreadState(doublePage = true, pageCountProvider = { pageCount })

        withSnapshot {
            state.onPageMeasured(pageIndex = 1, width = 2000f, height = 1000f, anchorPage = 0)
            state.clearRelocation()
        }
        pageCount = 12
        withSnapshot {
            state.syncPageCount()
            assertNull(state.relocateTo)
            assertEquals(setOf(1), state.layout.widePages, "续拉不应丢掉已测出的宽页")
        }
    }

    // ── 测试辅助 ────────────────────────────────────────────────────────

    private fun doublePageState(pageCount: Int) =
        PageSpreadState(doublePage = true, pageCountProvider = { pageCount })

    private fun withSnapshot(block: () -> Unit) = Snapshot.withMutableSnapshot(block)
}
