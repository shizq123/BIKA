package com.shizq.bika.ui.comicinfo

import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.data.model.User
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 回复弹窗的 id 投影。
 *
 * 下游是 flatMapLatest + Pager：这个流每多发射一次，回复列表就被取消重建一次、
 * 回到第一页。所以"不该发射时不发射"本身就是要测的行为。
 */
class ViewingRepliesIdsTest {

    private fun comment(id: String) = Comment(
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
        likesCount = 0,
        isLiked = false,
    )

    private fun content(
        viewingReplies: Comment? = null,
        isLiked: Boolean = false,
    ) = UnitedDetailsUiState.Content(
        id = "comic1",
        detail = ComicDetail(id = "comic1", isLiked = isLiked),
        viewingReplies = viewingReplies,
    )

    @Test
    fun `Initialize 与 Error 投影为 null`() = runTest {
        val result = flowOf(
            UnitedDetailsUiState.Initialize,
            UnitedDetailsUiState.Error(RuntimeException("boom")),
        ).viewingRepliesIds().toList()

        // 两个都是 null，distinctUntilChanged 合成一次发射
        assertEquals(listOf(null), result)
    }

    @Test
    fun `展开回复后发射对应 id`() = runTest {
        val result = flowOf(
            UnitedDetailsUiState.Initialize,
            content(),
            content(viewingReplies = comment("c1")),
        ).viewingRepliesIds().toList()

        assertEquals(listOf(null, "c1"), result)
    }

    @Test
    fun `关闭回复后回到 null`() = runTest {
        val result = flowOf(
            content(viewingReplies = comment("c1")),
            content(viewingReplies = null),
        ).viewingRepliesIds().toList()

        // 原实现用 filterNotNull，这里会只剩 "c1"，
        // 下游永远回不到"没有回复在看"的状态、Pager 一直挂着
        assertEquals(listOf("c1", null), result)
    }

    @Test
    fun `漫画点赞导致的 state 变化不触发发射`() = runTest {
        val target = comment("c1")
        val result = flowOf(
            content(viewingReplies = target, isLiked = false),
            // 用户点赞漫画：state 变了，但在看的回复没变
            content(viewingReplies = target, isLiked = true),
            content(viewingReplies = target, isLiked = false),
        ).viewingRepliesIds().toList()

        // 这是 #5 的核心：原实现缺 distinctUntilChanged，
        // 每次点赞都会让回复列表被拉回第一页
        assertEquals(listOf("c1"), result)
    }

    @Test
    fun `切换到另一条评论时发射新 id`() = runTest {
        val result = flowOf(
            content(viewingReplies = comment("c1")),
            content(viewingReplies = comment("c2")),
        ).viewingRepliesIds().toList()

        assertEquals(listOf("c1", "c2"), result)
    }

    @Test
    fun `相同 id 的不同 Comment 实例不触发重建`() = runTest {
        // 评论列表刷新后 Comment 实例会变（点赞数更新等），
        // 但只要还在看同一条评论，Pager 就不该重建
        val result = flowOf(
            content(viewingReplies = comment("c1")),
            content(viewingReplies = comment("c1")),
        ).viewingRepliesIds().toList()

        assertEquals(listOf("c1"), result)
    }

    @Test
    fun `关闭后重新打开同一条会重新发射`() = runTest {
        val result = flowOf(
            content(viewingReplies = comment("c1")),
            content(viewingReplies = null),
            content(viewingReplies = comment("c1")),
        ).viewingRepliesIds().toList()

        // 中间经过 null，所以第二次打开是一次真实的状态变化，
        // 需要重建 Pager 以拉取最新回复
        assertEquals(listOf("c1", null, "c1"), result)
    }
}
