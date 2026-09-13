package com.shizq.bika.feature.reader.impl.layout

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 跨页分组是纯函数，但它是 Pager 页码体系的地基：
 * pagerState 的 pageCount 取自 [buildPageSpreads] 的结果长度，进度保存取
 * [PageSpread.startIndex]，跳转靠 [spreadIndexOfPage] 反查。三者只要有一处
 * 与另两处理解不一致，表现就是"页码悄悄错位若干页"——不崩、不报错，
 * 只有用户发现进度对不上。这里把分组契约逐条钉死。
 */
class PageSpreadTest {

    // ── 单页模式 ────────────────────────────────────────────────────────

    @Test
    fun `单页模式每页独立成组`() {
        val spreads =
            buildPageSpreads(pageCount = 3, doublePage = false, widePageIndices = emptySet())

        assertEquals(3, spreads.size)
        assertEquals(listOf(0, 1, 2), spreads.map { it.startIndex })
        assertTrue(spreads.all { it is PageSpread.Single })
    }

    @Test
    fun `单页模式忽略宽页集合`() {
        // 单页模式下每屏本就只有一页，宽页不需要特殊处理；
        // 若这里错误地让宽页参与分组，会凭空多出或少掉翻页单位。
        val spreads =
            buildPageSpreads(pageCount = 3, doublePage = false, widePageIndices = setOf(1))

        assertEquals(3, spreads.size)
        assertTrue(spreads.all { it is PageSpread.Single })
    }

    @Test
    fun `页数为 0 或负数时返回空列表`() {
        // pagerState 的 pageCount 直接取这个长度，返回非空会让 Pager 渲染不存在的页。
        assertTrue(buildPageSpreads(0, doublePage = false, widePageIndices = emptySet()).isEmpty())
        assertTrue(buildPageSpreads(0, doublePage = true, widePageIndices = emptySet()).isEmpty())
        assertTrue(buildPageSpreads(-1, doublePage = true, widePageIndices = emptySet()).isEmpty())
    }

    // ── 双页模式：无宽页 ────────────────────────────────────────────────

    @Test
    fun `双页模式相邻两页配对`() {
        val spreads =
            buildPageSpreads(pageCount = 4, doublePage = true, widePageIndices = emptySet())

        assertEquals(2, spreads.size)
        assertEquals(PageSpread.Double(startIndex = 0, secondIndex = 1), spreads[0])
        assertEquals(PageSpread.Double(startIndex = 2, secondIndex = 3), spreads[1])
    }

    @Test
    fun `双页模式奇数页时末页落单`() {
        val spreads =
            buildPageSpreads(pageCount = 5, doublePage = true, widePageIndices = emptySet())

        assertEquals(3, spreads.size)
        assertEquals(PageSpread.Double(0, 1), spreads[0])
        assertEquals(PageSpread.Double(2, 3), spreads[1])
        assertEquals(PageSpread.Single(4), spreads[2])
    }

    @Test
    fun `双页模式只有一页时不越界`() {
        val spreads =
            buildPageSpreads(pageCount = 1, doublePage = true, widePageIndices = emptySet())

        assertEquals(listOf(PageSpread.Single(0)), spreads)
    }

    // ── 双页模式：宽页 ──────────────────────────────────────────────────

    @Test
    fun `宽页独占一组且不与邻页配对`() {
        // 这是引入 PageSpread 的初衷：旧实现用 index * 2 反推左右页，
        // 宽页独占一屏后其后所有页码整体错位，中间那页被直接跳过。
        val spreads = buildPageSpreads(pageCount = 4, doublePage = true, widePageIndices = setOf(1))

        assertEquals(3, spreads.size)
        assertEquals(PageSpread.Single(0), spreads[0], "第 0 页因邻页是宽页而落单")
        assertEquals(PageSpread.Single(1), spreads[1], "宽页独占")
        assertEquals(PageSpread.Double(2, 3), spreads[2])
    }

    @Test
    fun `首页为宽页时后续配对不错位`() {
        val spreads = buildPageSpreads(pageCount = 5, doublePage = true, widePageIndices = setOf(0))

        assertEquals(3, spreads.size)
        assertEquals(PageSpread.Single(0), spreads[0])
        assertEquals(PageSpread.Double(1, 2), spreads[1])
        assertEquals(PageSpread.Double(3, 4), spreads[2])
    }

    @Test
    fun `连续宽页各自独占`() {
        val spreads =
            buildPageSpreads(pageCount = 4, doublePage = true, widePageIndices = setOf(1, 2))

        assertEquals(4, spreads.size)
        assertTrue(spreads.all { it is PageSpread.Single })
        assertEquals(listOf(0, 1, 2, 3), spreads.map { it.startIndex })
    }

    @Test
    fun `全部页都是宽页时退化为单页布局`() {
        val spreads =
            buildPageSpreads(pageCount = 3, doublePage = true, widePageIndices = setOf(0, 1, 2))

        assertEquals(3, spreads.size)
        assertTrue(spreads.all { it is PageSpread.Single })
    }

    @Test
    fun `任何分组下每页恰好出现一次`() {
        // 最关键的不变量：漏页会让某页永远无法翻到，重复会让 Pager 的 key 冲突崩溃。
        val pageCount = 9
        for (wide in listOf(
            emptySet(),
            setOf(0),
            setOf(4),
            setOf(8),
            setOf(0, 3, 8),
            setOf(3, 4)
        )) {
            val spreads = buildPageSpreads(pageCount, doublePage = true, widePageIndices = wide)
            val covered = spreads.flatMap { spread ->
                when (spread) {
                    is PageSpread.Single -> listOf(spread.startIndex)
                    is PageSpread.Double -> listOf(spread.startIndex, spread.secondIndex)
                }
            }
            assertEquals(
                (0 until pageCount).toList(),
                covered.sorted(),
                "wide=$wide 时页覆盖不完整或有重复: $covered",
            )
        }
    }

    // ── containsPage ───────────────────────────────────────────────────

    @Test
    fun `containsPage 覆盖双页的两侧`() {
        val double = PageSpread.Double(startIndex = 2, secondIndex = 3)

        assertTrue(double.containsPage(2))
        assertTrue(double.containsPage(3), "右页也必须算命中，否则跳转到右页会落到下一屏")
        assertFalse(double.containsPage(1))
        assertFalse(double.containsPage(4))
    }

    @Test
    fun `containsPage 对单页只认自身`() {
        val single = PageSpread.Single(startIndex = 5)

        assertTrue(single.containsPage(5))
        assertFalse(single.containsPage(4))
        assertFalse(single.containsPage(6))
    }

    // ── spreadIndexOfPage ──────────────────────────────────────────────

    @Test
    fun `页码反查翻页单位下标`() {
        val spreads =
            buildPageSpreads(pageCount = 6, doublePage = true, widePageIndices = emptySet())

        assertEquals(0, spreads.spreadIndexOfPage(0))
        assertEquals(0, spreads.spreadIndexOfPage(1))
        assertEquals(1, spreads.spreadIndexOfPage(2))
        assertEquals(1, spreads.spreadIndexOfPage(3))
        assertEquals(2, spreads.spreadIndexOfPage(4))
        assertEquals(2, spreads.spreadIndexOfPage(5))
    }

    @Test
    fun `含宽页时反查仍然准确`() {
        val spreads = buildPageSpreads(pageCount = 5, doublePage = true, widePageIndices = setOf(2))
        // 分组为 D(0,1), S(2), D(3,4)：0 与 1 正常配对，宽页 2 独占，之后 3 与 4 重新配对。
        assertEquals(PageSpread.Double(0, 1), spreads[0])
        assertEquals(PageSpread.Single(2), spreads[1])
        assertEquals(PageSpread.Double(3, 4), spreads[2])

        assertEquals(1, spreads.spreadIndexOfPage(2))
        assertEquals(2, spreads.spreadIndexOfPage(3))
        assertEquals(2, spreads.spreadIndexOfPage(4))
    }

    @Test
    fun `空分组返回 0 而不是崩溃`() {
        // 章节切换后 itemCount 短暂为 0，此时进度恢复可能已经在调 scrollToPage。
        assertEquals(0, emptyList<PageSpread>().spreadIndexOfPage(3))
    }

    @Test
    fun `负页码归到第一组`() {
        val spreads =
            buildPageSpreads(pageCount = 4, doublePage = true, widePageIndices = emptySet())

        assertEquals(0, spreads.spreadIndexOfPage(-1))
        assertEquals(0, spreads.spreadIndexOfPage(Int.MIN_VALUE))
    }

    @Test
    fun `越界页码归到最后一组`() {
        // 数据尚未加载到目标页时会出现：宁可停在末尾，也不能抛异常或滚到第一页。
        val spreads =
            buildPageSpreads(pageCount = 4, doublePage = true, widePageIndices = emptySet())

        assertEquals(spreads.lastIndex, spreads.spreadIndexOfPage(99))
        assertEquals(spreads.lastIndex, spreads.spreadIndexOfPage(Int.MAX_VALUE))
    }

    @Test
    fun `分组与反查互为逆运算`() {
        // scrollToPage 依赖这个性质：拿页码查到单位下标，该单位必须真的含这一页。
        val spreads =
            buildPageSpreads(pageCount = 11, doublePage = true, widePageIndices = setOf(1, 6))

        for (page in 0 until 11) {
            val index = spreads.spreadIndexOfPage(page)
            assertTrue(
                spreads[index].containsPage(page),
                "第 $page 页查到单位 $index，但该单位不含此页: ${spreads[index]}",
            )
        }
    }

    // ── 宽页判定 ────────────────────────────────────────────────────────

    @Test
    fun `宽高比超过阈值判定为宽页`() {
        assertTrue(isWidePage(width = 2000f, height = 1000f))
        assertTrue(isWidePage(width = 1200f, height = 1000f), "1.2 > 1.1 阈值")
    }

    @Test
    fun `轻微超宽的普通页不算宽页`() {
        // 阈值取 1.1 而非 1.0 正是为了容忍这种页；退回 1.0 会让大量普通页被误判独占。
        assertFalse(isWidePage(width = 1050f, height = 1000f))
        assertFalse(isWidePage(width = 1000f, height = 1000f), "正方形不算")
        assertFalse(isWidePage(width = 700f, height = 1000f), "竖图不算")
    }

    @Test
    fun `尺寸非法时不判定为宽页`() {
        // 解码失败或占位状态会上报 0，误判成宽页会打乱分组。
        assertFalse(isWidePage(width = 0f, height = 1000f))
        assertFalse(isWidePage(width = 1000f, height = 0f))
        assertFalse(isWidePage(width = -2000f, height = 1000f))
    }
}
