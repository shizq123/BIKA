package com.shizq.bika.feature.reader.impl.layout

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * 「分组变化 × 当前位置」的交互契约。
 *
 * 这组测试之前无法存在：分组住在 PageSpreadState（模块持有），位置住在 PagerState
 * （Compose 持有），二者的协调分散在一个 LaunchedEffect、一次破坏性 consume 和一个
 * 异步回调里，只有跑起真实 Compose 运行时才能观察。而模块里最容易错的地方恰好全在
 * 这个交界上。把它收成纯函数之后，这里就是它唯一的行为定义。
 */
class SpreadLayoutTest {

    @Test
    fun `重定位判据是单位下标是否真的变了`() {
        // 10 页双页：D(0,1) D(2,3) D(4,5) D(6,7) D(8,9)，用户在第 6 页（下标 3）。
        val layout = SpreadLayout.of(pageCount = 10, doublePage = true)
        assertEquals(3, layout.spreads.spreadIndexOfPage(6))

        // 第 1 页被测出是宽页 → S(0) S(1) D(2,3) D(4,5) D(6,7) D(8,9)
        // 第 6 页现在在下标 4，视口必须跟着挪，否则用户会看到第 8 页。
        val result = layout.withMeasurement(1, width = 2000f, height = 1000f, anchorPage = 6)

        assertEquals(4, result.layout.spreads.spreadIndexOfPage(6))
        assertEquals(6, result.relocateTo)
    }

    @Test
    fun `当前位置之后的宽页不产生重定位`() {
        val layout = SpreadLayout.of(pageCount = 10, doublePage = true)

        val result = layout.withMeasurement(8, width = 2000f, height = 1000f, anchorPage = 2)

        assertEquals(1, result.layout.spreads.spreadIndexOfPage(2), "当前屏下标不变")
        assertNull(result.relocateTo, "多余的重定位就是用户眼里的一次画面跳动")
    }

    @Test
    fun `anchor 自身是宽页但下标不变时不重定位`() {
        // 精确判据与旧的位置判据在这里分道扬镳。
        // D(0,1) D(2,3)… 中第 2 页被测出是宽页 → D(0,1) S(2) S(3) D(4,5)…
        // 第 2 页重排前后都在下标 1，视口无需动。旧判据（宽页位置 >= anchor 就记）
        // 会记下 anchor，渲染层再发现 target == currentPage 又跳过滚动，净效果相同，
        // 只是白绕一圈。
        val layout = SpreadLayout.of(pageCount = 10, doublePage = true)

        val result = layout.withMeasurement(2, width = 2000f, height = 1000f, anchorPage = 2)

        assertEquals(1, layout.spreads.spreadIndexOfPage(2))
        assertEquals(1, result.layout.spreads.spreadIndexOfPage(2))
        assertNull(result.relocateTo)
    }

    @Test
    fun `anchor 是配对右页且自身变宽页时需要重定位`() {
        // D(0,1) 中第 1 页被测出是宽页 → S(0) S(1)：第 1 页从下标 0 挪到 1。
        val layout = SpreadLayout.of(pageCount = 6, doublePage = true)

        val result = layout.withMeasurement(1, width = 2000f, height = 1000f, anchorPage = 1)

        assertEquals(0, layout.spreads.spreadIndexOfPage(1))
        assertEquals(1, result.layout.spreads.spreadIndexOfPage(1))
        assertEquals(1, result.relocateTo)
    }

    @Test
    fun `重复上报同一宽页是幂等的`() {
        val layout = SpreadLayout.of(pageCount = 10, doublePage = true)
        val first = layout.withMeasurement(1, 2000f, 1000f, anchorPage = 6)

        val second = first.layout.withMeasurement(1, 2000f, 1000f, anchorPage = 6)

        assertSame(first.layout, second.layout, "无变化时返回同一实例，调用方可据此跳过写入")
        assertNull(second.relocateTo)
    }

    @Test
    fun `单页模式不受宽页影响`() {
        val layout = SpreadLayout.of(pageCount = 6, doublePage = false)

        val result = layout.withMeasurement(2, 2000f, 1000f, anchorPage = 4)

        assertSame(layout, result.layout)
        assertNull(result.relocateTo)
    }

    @Test
    fun `页数增长保留已测出的宽页且不重定位`() {
        val layout = SpreadLayout.of(pageCount = 4, doublePage = true)
            .withMeasurement(1, 2000f, 1000f, anchorPage = null).layout

        val grown = layout.withPageCount(12)

        assertEquals(setOf(1), grown.widePages)
        assertEquals(12, grown.pageCount)
        // 前面的分组边界没变，所以任何已有位置的下标都不受影响。
        assertEquals(
            layout.spreads.spreadIndexOfPage(3),
            grown.spreads.spreadIndexOfPage(3),
        )
    }

    @Test
    fun `positionAt 给出一屏覆盖的真实页码范围`() {
        val layout = SpreadLayout.of(pageCount = 10, doublePage = true)

        val position = layout.positionAt(4)

        // 末屏 D(8,9)：起始页 8 用于进度，末页 9 用于「读完没」。
        assertEquals(ReadingPositionSnapshot(8, 9), position)
        assertEquals(8, position?.forProgress)
        assertEquals(9, position?.forEndOfChapter)
    }

    @Test
    fun `positionAt 对宽页给出单页范围`() {
        val layout = SpreadLayout.of(pageCount = 10, doublePage = true, widePages = setOf(0))

        assertEquals(ReadingPositionSnapshot(0, 0), layout.positionAt(0))
    }

    @Test
    fun `positionAt 越界返回 null`() {
        val layout = SpreadLayout.of(pageCount = 4, doublePage = true)

        assertNull(layout.positionAt(99))
        assertNull(SpreadLayout.of(pageCount = 0, doublePage = true).positionAt(0))
    }

    @Test
    fun `末屏落单页的末页判定`() {
        // 9 页：D(0,1) D(2,3) D(4,5) D(6,7) S(8)
        val layout = SpreadLayout.of(pageCount = 9, doublePage = true)

        assertEquals(ReadingPositionSnapshot(8, 8), layout.positionAt(4))
    }

    @Test
    fun `无效页码上报被忽略`() {
        val layout = SpreadLayout.of(pageCount = 4, doublePage = true)

        assertSame(layout, layout.withMeasurement(-1, 2000f, 1000f, 0).layout)
        assertSame(layout, layout.withMeasurement(99, 2000f, 1000f, 0).layout)
        assertSame(layout, layout.withMeasurement(1, 0f, 0f, 0).layout)
    }
}
