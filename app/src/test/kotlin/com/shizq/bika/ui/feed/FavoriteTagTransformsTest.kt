package com.shizq.bika.ui.feed

import com.shizq.bika.core.model.FavoriteTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class FavoriteTagTransformsTest {

    private fun tag(name: String, type: String = FeedActionType.Channel.storageValue) =
        FavoriteTag(name = name, actionType = type)

    @Test
    fun `添加时防止重复标签`() {
        val tags = listOf(tag("A"))
        assertSame(tags, addFavoriteTagToList(tags, tag("A")))
        assertEquals(listOf(tag("A"), tag("B")), addFavoriteTagToList(tags, tag("B")))
    }

    @Test
    fun `删除只移除匹配标签`() {
        val tags = listOf(tag("A"), tag("B"))
        assertEquals(listOf(tag("B")), removeFavoriteTagFromList(tags, tag("A")))
    }

    @Test
    fun `改名规范化名称并保留其余字段`() {
        val original = FavoriteTag(name = "旧名称", actionType = "Knight", actionId = "id")
        assertEquals(
            listOf(original.copy(name = "新名称")),
            renameFavoriteTagInList(listOf(original), original, "  新名称  "),
        )
    }

    @Test
    fun `空白改名和越界移动原样返回`() {
        val tags = listOf(tag("A"), tag("B"))
        assertSame(tags, renameFavoriteTagInList(tags, tag("A"), "  "))
        assertSame(tags, moveFavoriteTagInList(tags, -1, 1))
        assertSame(tags, moveFavoriteTagInList(tags, 0, 0))
    }

    @Test
    fun `移动落点为目标下标`() {
        val tags = listOf(tag("A"), tag("B"), tag("C"))
        assertEquals(
            listOf("B", "C", "A"),
            moveFavoriteTagInList(tags, 0, 2).map { it.name },
        )
    }
}
