package com.shizq.bika.ui.comicinfo

import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.data.model.User
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * 置顶评论的补发判定。
 *
 * 这条逻辑存在的原因是一个时序竞态：评论第一页与漫画详情并发请求，
 * 若评论先返回，状态机还在 Initialize，TopCommentsLoaded 没有匹配的
 * inState 会被静默丢弃，置顶评论永远不显示。
 */
class ReplayTopCommentsTest {

    private fun user() = User(
        id = "u1",
        name = "用户",
        gender = "m",
        title = "",
        slogan = "",
        level = 1,
        exp = 0,
        avatar = null,
        characters = emptyList(),
    )

    private fun comment(id: String, likesCount: Int = 0) = Comment(
        id = id,
        content = "内容",
        user = user(),
        totalComments = 0,
        createdAt = "刚刚",
        likesCount = likesCount,
        isLiked = false,
    )

    @Test
    fun `没有缓存时不补发`() {
        // 评论还没加载完，没什么可补的
        assertFalse(shouldReplayTopComments(cached = null, inState = emptyList()))
    }

    @Test
    fun `缓存为空列表且状态也为空时不补发`() {
        // 这本漫画确实没有置顶评论，补发一次空列表是无意义的 mutate
        assertFalse(shouldReplayTopComments(cached = emptyList(), inState = emptyList()))
    }

    @Test
    fun `缓存有值而状态为空时补发`() {
        // 竞态发生了：评论先到、action 被丢弃，状态里还是空的
        assertTrue(
            shouldReplayTopComments(
                cached = listOf(comment("p1")),
                inState = emptyList(),
            )
        )
    }

    @Test
    fun `状态已有相同置顶评论时不补发`() {
        // 详情先返回的正常路径：action 已被状态机接收
        assertFalse(
            shouldReplayTopComments(
                cached = listOf(comment("p1"), comment("p2")),
                inState = listOf(comment("p1"), comment("p2")),
            )
        )
    }

    @Test
    fun `置顶评论内容变化但 id 相同时不补发`() {
        // 只有 id 序列决定"是不是同一批置顶评论"。
        // 点赞数之类的字段差异不该触发补发。
        assertFalse(
            shouldReplayTopComments(
                cached = listOf(comment("p1", likesCount = 5)),
                inState = listOf(comment("p1", likesCount = 3)),
            )
        )
    }

    @Test
    fun `id 序列不同时补发`() {
        assertTrue(
            shouldReplayTopComments(
                cached = listOf(comment("p1"), comment("p2")),
                inState = listOf(comment("p1")),
            )
        )
    }

    @Test
    fun `顺序不同时补发`() {
        // 置顶评论的顺序是服务端给的，顺序变化意味着是新的一批
        assertTrue(
            shouldReplayTopComments(
                cached = listOf(comment("p1"), comment("p2")),
                inState = listOf(comment("p2"), comment("p1")),
            )
        )
    }

    @Test
    fun `缓存为空而状态有值时补发`() {
        // 评论刷新后置顶被撤下，状态需要跟着清空
        assertTrue(
            shouldReplayTopComments(
                cached = emptyList(),
                inState = listOf(comment("p1")),
            )
        )
    }

    /**
     * 这条用例锁的是"为什么不能直接用 == 比较列表"。
     *
     * Comment 是 data class，它生成的 equals 会调用 User.equals；
     * 而 User 是普通 class，没有实现 equals，走的是引用相等。
     * 两次网络解析出的 User 是不同实例，所以内容完全相同的两条 Comment
     * 也不相等——若用 `cached != inState` 判断，会每次都补发。
     */
    @Test
    fun `内容相同但 User 实例不同的 Comment 并不相等`() {
        assertNotEquals(comment("p1"), comment("p1"))

        // 正因如此，判定必须按 id 比较
        assertFalse(
            shouldReplayTopComments(
                cached = listOf(comment("p1")),
                inState = listOf(comment("p1")),
            )
        )
    }
}
