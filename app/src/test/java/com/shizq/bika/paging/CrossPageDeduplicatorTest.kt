package com.shizq.bika.paging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 跨页去重。
 *
 * 这层是 UI 侧 key 能回归纯 id 的前提：只要还存在重复 id 下发到
 * LazyColumn，就必须靠 index 兜底，而掺 index 的 key 会让插入数据后
 * 所有后续 item 的身份发生变化。
 */
class CrossPageDeduplicatorTest {

    private data class Item(val id: String, val label: String = "")

    private fun deduplicator() = CrossPageDeduplicator<Item> { it.id }

    @Test
    fun `首页全部保留`() {
        val result = deduplicator().retainUnseen(
            listOf(Item("a"), Item("b"), Item("c"))
        )

        assertEquals(listOf("a", "b", "c"), result.map { it.id })
    }

    @Test
    fun `保持原有顺序`() {
        val result = deduplicator().retainUnseen(
            listOf(Item("c"), Item("a"), Item("b"))
        )

        // 去重不能顺带排序：章节列表依赖服务端返回的顺序
        assertEquals(listOf("c", "a", "b"), result.map { it.id })
    }

    @Test
    fun `第二页中与首页重复的条目被丢弃`() {
        val dedup = deduplicator()
        dedup.retainUnseen(listOf(Item("a"), Item("b")))

        // 服务端分页抖动：b 同时落在第一页和第二页
        val page2 = dedup.retainUnseen(listOf(Item("b"), Item("c")))

        assertEquals(listOf("c"), page2.map { it.id })
    }

    @Test
    fun `同一页内的重复也会被去掉`() {
        val result = deduplicator().retainUnseen(
            listOf(Item("a"), Item("a"), Item("b"))
        )

        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    @Test
    fun `整页都是重复时返回空列表`() {
        val dedup = deduplicator()
        dedup.retainUnseen(listOf(Item("a"), Item("b")))

        val page2 = dedup.retainUnseen(listOf(Item("a"), Item("b")))

        // 这是需要留意的后果：返回条数小于 pageSize，Paging 会继续请求下一页。
        // 只要 nextKey 仍然推进就不会死循环。
        assertTrue(page2.isEmpty())
    }

    @Test
    fun `空页不影响已见集合`() {
        val dedup = deduplicator()
        dedup.retainUnseen(listOf(Item("a")))

        assertTrue(dedup.retainUnseen(emptyList()).isEmpty())
        assertEquals(listOf("b"), dedup.retainUnseen(listOf(Item("b"))).map { it.id })
    }

    @Test
    fun `保留的是首次出现的那个实例`() {
        val dedup = deduplicator()
        val first = Item("a", label = "第一次")
        dedup.retainUnseen(listOf(first))

        val page2 = dedup.retainUnseen(listOf(Item("a", label = "第二次"), Item("b")))

        // 首次出现的已经在列表里了，后到的同 id 条目一律丢弃，
        // 不会出现"同一位置内容被悄悄替换"
        assertEquals(listOf("b"), page2.map { it.id })
    }

    @Test
    fun `不同实例互不共享已见集合`() {
        val first = deduplicator()
        first.retainUnseen(listOf(Item("a")))

        // Paging invalidate 后会新建 PagingSource，去重状态必须随之重置，
        // 否则刷新后第一页会被整页丢弃
        val second = deduplicator()

        assertEquals(listOf("a"), second.retainUnseen(listOf(Item("a"))).map { it.id })
    }

    @Test
    fun `按自定义 id 提取器判重`() {
        val dedup = CrossPageDeduplicator<Item> { it.label }
        dedup.retainUnseen(listOf(Item("a", label = "x")))

        // id 不同但 label 相同，按 label 判重应被丢弃
        val result = dedup.retainUnseen(listOf(Item("b", label = "x")))

        assertTrue(result.isEmpty())
    }
}
