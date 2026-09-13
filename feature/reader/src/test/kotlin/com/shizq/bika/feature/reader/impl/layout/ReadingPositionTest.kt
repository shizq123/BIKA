package com.shizq.bika.feature.reader.impl.layout

import org.junit.Test
import kotlin.test.assertEquals

/**
 * 条漫「当前阅读到的页码」判定规则，从 WebtoonController.calculateCurrentPageIndex
 * 中抽出。原实现内嵌在 Composable 里，依赖真实 LazyListState 才能验证边界
 * （空列表、末页较短、跨页），单测困难；抽成纯函数后逐条钉死。
 */
class ReadingPositionTest {

    @Test
    fun `视口为空时沿用上次有效值`() {
        val layoutInfo = ListReadingLayoutInfo(
            firstVisibleItem = null,
            lastVisibleItem = null,
            totalItemsCount = 10,
            viewportEndOffset = 1000,
        )

        assertEquals(3, resolveListReadingPosition(layoutInfo, lastValidIndex = 3))
    }

    @Test
    fun `数据未到达(totalItemsCount 为 0)时沿用上次有效值`() {
        // 章节切换后 itemCount 短暂为 0，此时不能返回 0：
        // 会被当成「用户在第一页」写入进度，覆盖真实位置。
        val layoutInfo = ListReadingLayoutInfo(
            firstVisibleItem = VisibleItemSnapshot(index = 5, offset = 0, size = 100),
            lastVisibleItem = VisibleItemSnapshot(index = 5, offset = 0, size = 100),
            totalItemsCount = 0,
            viewportEndOffset = 1000,
        )

        assertEquals(7, resolveListReadingPosition(layoutInfo, lastValidIndex = 7))
    }

    @Test
    fun `末页完全可见时强制视为最后一页`() {
        // 解决最后一页较短、视口中心线永远落不到它上面导致无法触发已读的问题。
        val layoutInfo = ListReadingLayoutInfo(
            firstVisibleItem = VisibleItemSnapshot(index = 8, offset = -500, size = 800),
            lastVisibleItem = VisibleItemSnapshot(index = 9, offset = 300, size = 400),
            totalItemsCount = 10,
            viewportEndOffset = 1000,
        )

        assertEquals(9, resolveListReadingPosition(layoutInfo, lastValidIndex = 0))
    }

    @Test
    fun `末页可见但底边超出视口时不强制到底`() {
        // 最后一项刚进入视口顶部，底边还在视口外：还没真正滚到底，
        // 应该按普通规则取 firstVisibleItem，而不是提前判定为已读完。
        val layoutInfo = ListReadingLayoutInfo(
            firstVisibleItem = VisibleItemSnapshot(index = 9, offset = 200, size = 2000),
            lastVisibleItem = VisibleItemSnapshot(index = 9, offset = 200, size = 2000),
            totalItemsCount = 10,
            viewportEndOffset = 1000,
        )

        assertEquals(9, resolveListReadingPosition(layoutInfo, lastValidIndex = 0))
    }

    @Test
    fun `最后一项可见但不是末页时不触发到底判定`() {
        val layoutInfo = ListReadingLayoutInfo(
            firstVisibleItem = VisibleItemSnapshot(index = 3, offset = 0, size = 500),
            lastVisibleItem = VisibleItemSnapshot(index = 4, offset = 500, size = 300),
            totalItemsCount = 10,
            viewportEndOffset = 1000,
        )

        assertEquals(3, resolveListReadingPosition(layoutInfo, lastValidIndex = 0))
    }

    @Test
    fun `常规情况取第一个可见项而非中心线`() {
        // 条漫图片可能远超屏幕高度，中心线会指向更早的页，导致进度落后于用户实际阅读位置。
        val layoutInfo = ListReadingLayoutInfo(
            firstVisibleItem = VisibleItemSnapshot(index = 27, offset = -5000, size = 8000),
            lastVisibleItem = VisibleItemSnapshot(index = 27, offset = -5000, size = 8000),
            totalItemsCount = 50,
            viewportEndOffset = 1000,
        )

        assertEquals(27, resolveListReadingPosition(layoutInfo, lastValidIndex = 12))
    }

    @Test
    fun `恰好滚动到底边界值(offset+size等于viewportEndOffset)算完全可见`() {
        val layoutInfo = ListReadingLayoutInfo(
            firstVisibleItem = VisibleItemSnapshot(index = 9, offset = 600, size = 400),
            lastVisibleItem = VisibleItemSnapshot(index = 9, offset = 600, size = 400),
            totalItemsCount = 10,
            viewportEndOffset = 1000,
        )

        assertEquals(9, resolveListReadingPosition(layoutInfo, lastValidIndex = 0))
    }
}
