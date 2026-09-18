package com.shizq.bika.ui.comicinfo

import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.data.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 评论点赞覆盖层的合并规则。
 *
 * 这层存在的前提：PagingData 不可变，点赞结果没法写回已加载的页，
 * 所以额外维护 commentId -> 用户切换后的状态，在 map 阶段合并进列表。
 */
class LikeOverrideTest {

    private fun comment(
        id: String = "c1",
        isLiked: Boolean = false,
        likesCount: Int = 10,
    ) = Comment(
        id = id,
        content = "内容",
        user = User(
            id = "u1",
            name = "用户",
            gender = "m",
            title = "",
            slogan = "",
            level = 1,
            exp = 0,
            avatar = null,
            characters = emptyList(),
        ),
        totalComments = 0,
        createdAt = "刚刚",
        likesCount = likesCount,
        isLiked = isLiked,
    )

    @Test
    fun `没有覆盖时原样返回同一实例`() {
        val original = comment()

        // 返回同一实例而不是 copy：避免在 PagingData.map 里制造无意义的重组
        assertSame(original, original.applyLikeOverride(emptyMap()))
    }

    @Test
    fun `其他评论的覆盖不影响本条`() {
        val original = comment(id = "c1")

        assertSame(original, original.applyLikeOverride(mapOf("c2" to true)))
    }

    @Test
    fun `点赞后 isLiked 为真且计数加一`() {
        val result = comment(isLiked = false, likesCount = 10)
            .applyLikeOverride(mapOf("c1" to true))

        assertTrue(result.isLiked)
        assertEquals(11, result.likesCount)
    }

    @Test
    fun `取消点赞后 isLiked 为假且计数减一`() {
        val result = comment(isLiked = true, likesCount = 10)
            .applyLikeOverride(mapOf("c1" to false))

        assertFalse(result.isLiked)
        assertEquals(9, result.likesCount)
    }

    @Test
    fun `覆盖值与原始值相同时不重复计数`() {
        // 刷新后服务端返回的 isLiked 已是点赞后的值，此时覆盖层仍在。
        // 若无条件加一，计数会比真实值多一。
        val refreshed = comment(isLiked = true, likesCount = 11)

        val result = refreshed.applyLikeOverride(mapOf("c1" to true))

        assertSame(refreshed, result)
        assertEquals(11, result.likesCount)
    }

    @Test
    fun `取消点赞时相同值同样不重复减一`() {
        val refreshed = comment(isLiked = false, likesCount = 9)

        val result = refreshed.applyLikeOverride(mapOf("c1" to false))

        assertSame(refreshed, result)
        assertEquals(9, result.likesCount)
    }

    @Test
    fun `计数为零时取消点赞不会变成负数`() {
        // 服务端计数与本地不同步时（例如他人已取消），减一可能越界
        val result = comment(isLiked = true, likesCount = 0)
            .applyLikeOverride(mapOf("c1" to false))

        assertEquals(0, result.likesCount)
    }

    @Test
    fun `合并不改动评论的其他字段`() {
        val original = comment(isLiked = false, likesCount = 3)

        val result = original.applyLikeOverride(mapOf("c1" to true))

        assertEquals(original.id, result.id)
        assertEquals(original.content, result.content)
        assertEquals(original.createdAt, result.createdAt)
        assertEquals(original.totalComments, result.totalComments)
        // User 没有实现 equals，这里断言的是"沿用同一引用"而非结构相等
        assertSame(original.user, result.user)
    }

    @Test
    fun `连续两次相反覆盖回到原始计数`() {
        val original = comment(isLiked = false, likesCount = 10)

        // 模拟用户点赞后立刻取消：覆盖层被移除（回滚），而不是记 false
        val liked = original.applyLikeOverride(mapOf("c1" to true))
        val rolledBack = original.applyLikeOverride(emptyMap())

        assertEquals(11, liked.likesCount)
        assertEquals(10, rolledBack.likesCount)
        assertFalse(rolledBack.isLiked)
    }
}
