package com.shizq.bika.core.data.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChapterNavigationTest {

    private fun createChapter(order: Int, title: String = "第${order}话") = Chapter(
        id = "id_$order",
        order = order,
        title = title,
        updatedAt = ""
    )

    @Test
    fun `多章漫画在阅读第2话时下一章为第3话上一章为第1话`() {
        // 哔咔真实语义：第1话 order=1，第2话 order=2，第3话（最新话）order=3
        // 自然正序列表：第1话在最前，最新话在最后
        val ch1 = createChapter(order = 1, title = "第1话")
        val ch2 = createChapter(order = 2, title = "第2话")
        val ch3 = createChapter(order = 3, title = "第3话")
        val catalog = ChapterCatalog(
            chapters = listOf(ch1, ch2, ch3),
            isComplete = true
        )

        val nav = catalog.navigationAt(order = 2)

        assertEquals(ch1, nav.prev)
        assertEquals(ch3, nav.next)
        assertTrue(nav.isResolved)
    }

    @Test
    fun `阅读第1话时上一章为空下一章为第2话`() {
        val ch1 = createChapter(order = 1, title = "第1话")
        val ch2 = createChapter(order = 2, title = "第2话")
        val ch3 = createChapter(order = 3, title = "第3话")
        val catalog = ChapterCatalog(
            chapters = listOf(ch1, ch2, ch3),
            isComplete = true
        )

        val nav = catalog.navigationAt(order = 1)

        assertNull(nav.prev)
        assertEquals(ch2, nav.next)
        assertTrue(nav.isResolved)
    }

    @Test
    fun `阅读最后一话时上一章为第2话下一章为空`() {
        val ch1 = createChapter(order = 1, title = "第1话")
        val ch2 = createChapter(order = 2, title = "第2话")
        val ch3 = createChapter(order = 3, title = "第3话")
        val catalog = ChapterCatalog(
            chapters = listOf(ch1, ch2, ch3),
            isComplete = true
        )

        val nav = catalog.navigationAt(order = 3)

        assertEquals(ch2, nav.prev)
        assertNull(nav.next)
        assertTrue(nav.isResolved)
    }

    @Test
    fun `单话漫画上一章与下一章均为空`() {
        val single = createChapter(order = 1, title = "全一话")
        val catalog = ChapterCatalog(
            chapters = listOf(single),
            isComplete = true
        )

        val nav = catalog.navigationAt(order = 1)

        assertNull(nav.prev)
        assertNull(nav.next)
        assertTrue(nav.isResolved)
    }

    @Test
    fun `当前章节不在目录中时根据order大小正确回退相邻章节`() {
        val ch1 = createChapter(order = 1, title = "第1话")
        val ch3 = createChapter(order = 3, title = "第3话")
        // 目录中缺失 order=2 的第2话
        val catalog = ChapterCatalog(
            chapters = listOf(ch1, ch3),
            isComplete = true
        )

        val nav = catalog.navigationAt(order = 2)

        // order 较小者为上一章（ch1），order 较大者为下一章（ch3）
        assertEquals(ch1, nav.prev)
        assertEquals(ch3, nav.next)
    }

    @Test
    fun `目录未加载完成时isResolved为false`() {
        val ch1 = createChapter(order = 1, title = "第1话")
        val catalog = ChapterCatalog(
            chapters = listOf(ch1),
            isComplete = false
        )

        val nav = catalog.navigationAt(order = 1)

        assertFalse(nav.isResolved)
    }
}
