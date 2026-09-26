package com.shizq.bika.ui.dashboard

import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.ui.feed.FeedActionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * 收藏标签整表变换的单测。
 *
 * 这些 transform 会在 DataStore 事务内执行，一旦写错就是持久化的脏数据，
 * 所以覆盖的重点是边界：去重的身份判定、空名守卫、越界下标、移动的落点。
 */
class FavoriteTagTransformsTest {

    private fun channelTag(name: String) = FavoriteTag(
        name = name,
        actionType = FeedActionType.Channel.storageValue,
    )

    private fun knightTag(name: String, id: String = "k1") = FavoriteTag(
        name = name,
        actionType = FeedActionType.Knight.storageValue,
        actionId = id,
    )

    // ── add ──────────────────────────────────────────────────────────────

    @Test
    fun `新标签追加到末尾`() {
        val tags = listOf(channelTag("A"))
        assertEquals(
            listOf(channelTag("A"), channelTag("B")),
            addFavoriteTag(tags, channelTag("B")),
        )
    }

    /**
     * 快速连点收藏按钮时会连续 dispatch 多次 AddFavoriteTag，去重必须在
     * transform 里做（事务内），而不是靠 UI 层禁用按钮。
     */
    @Test
    fun `同名同类型不重复添加 且原样返回同一实例`() {
        val tags = listOf(channelTag("A"))
        val result = addFavoriteTag(tags, channelTag("A"))
        assertSame(tags, result)
    }

    /**
     * 身份判定是 name + actionType，不含 actionId。同名但类型不同应视为两个标签
     * ——频道「原创」和骑士「原创」是不同入口。
     */
    @Test
    fun `同名但类型不同视为两个标签`() {
        val tags = listOf(channelTag("原创"))
        val result = addFavoriteTag(tags, knightTag("原创"))
        assertEquals(2, result.size)
    }

    /**
     * actionId 不参与身份判定：同名同类型、仅 actionId 不同时按重复处理。
     * 这条钉住 isSameTag 的语义，防止有人「顺手」把 actionId 加进比较——
     * 那会让早期无 actionId 的历史标签与新写入的同一标签共存。
     */
    @Test
    fun `actionId 不同但名称类型相同仍算重复`() {
        val tags = listOf(knightTag("骑士榜", id = ""))
        val result = addFavoriteTag(tags, knightTag("骑士榜", id = "k9"))
        assertSame(tags, result)
    }

    // ── remove ───────────────────────────────────────────────────────────

    @Test
    fun `按身份移除 只删中的那一个`() {
        val tags = listOf(channelTag("A"), knightTag("A"), channelTag("B"))
        assertEquals(
            listOf(knightTag("A"), channelTag("B")),
            removeFavoriteTag(tags, channelTag("A")),
        )
    }

    @Test
    fun `移除不存在的标签得到等价列表`() {
        val tags = listOf(channelTag("A"))
        assertEquals(tags, removeFavoriteTag(tags, channelTag("Z")))
    }

    // ── rename ───────────────────────────────────────────────────────────

    @Test
    fun `改名只改命中的那一个`() {
        val tags = listOf(channelTag("A"), channelTag("B"))
        assertEquals(
            listOf(channelTag("A2"), channelTag("B")),
            renameFavoriteTag(tags, channelTag("A"), "A2"),
        )
    }

    /**
     * 空名会在 UI 上留下一个不可点击的空条目，且之后无法再定位它来删除。
     * 纯空格同样要挡住——isBlank 而非 isEmpty。
     */
    @Test
    fun `空名和纯空格都不改名`() {
        val tags = listOf(channelTag("A"))
        assertSame(tags, renameFavoriteTag(tags, channelTag("A"), ""))
        assertSame(tags, renameFavoriteTag(tags, channelTag("A"), "   "))
    }

    @Test
    fun `改名保留 actionId`() {
        val tags = listOf(knightTag("骑士榜", id = "k7"))
        val result = renameFavoriteTag(tags, knightTag("骑士榜", id = "k7"), "我的骑士")
        assertEquals(listOf(knightTag("我的骑士", id = "k7")), result)
    }

    // ── move ─────────────────────────────────────────────────────────────

    private val abcd = listOf(
        channelTag("A"), channelTag("B"), channelTag("C"), channelTag("D"),
    )

    /**
     * 往后移：removeAt 会让其后元素左移一位，随后在原目标下标插入，
     * 落点仍是调用方期望的位置。这条是 `add(toIndex, removeAt(fromIndex))`
     * 这行代码唯一容易被误读的地方。
     */
    @Test
    fun `往后移动 落点为目标下标`() {
        val result = moveFavoriteTag(abcd, fromIndex = 0, toIndex = 2)
        assertEquals(listOf("B", "C", "A", "D"), result.map { it.name })
        assertEquals(2, result.indexOfFirst { it.name == "A" })
    }

    @Test
    fun `往前移动 落点为目标下标`() {
        val result = moveFavoriteTag(abcd, fromIndex = 3, toIndex = 1)
        assertEquals(listOf("A", "D", "B", "C"), result.map { it.name })
        assertEquals(1, result.indexOfFirst { it.name == "D" })
    }

    @Test
    fun `移到自身位置不变`() {
        assertEquals(abcd, moveFavoriteTag(abcd, fromIndex = 1, toIndex = 1))
    }

    /**
     * 拖拽手势可能带着过期下标到达（列表在手势进行中被 DataStore 推送更新过），
     * 越界必须原样返回而不是抛 IndexOutOfBounds——那会让整个 DataStore 事务失败。
     */
    @Test
    fun `任一下标越界时原样返回`() {
        assertSame(abcd, moveFavoriteTag(abcd, fromIndex = -1, toIndex = 1))
        assertSame(abcd, moveFavoriteTag(abcd, fromIndex = 0, toIndex = 4))
        assertSame(abcd, moveFavoriteTag(abcd, fromIndex = 9, toIndex = 0))

        // 空列表的 indices 为空区间，任何下标都越界
        val empty = emptyList<FavoriteTag>()
        assertSame(empty, moveFavoriteTag(empty, fromIndex = 0, toIndex = 0))
    }

    @Test
    fun `移动不改变元素个数`() {
        val result = moveFavoriteTag(abcd, fromIndex = 0, toIndex = 3)
        assertEquals(abcd.size, result.size)
        assertEquals(abcd.toSet(), result.toSet())
    }
}
