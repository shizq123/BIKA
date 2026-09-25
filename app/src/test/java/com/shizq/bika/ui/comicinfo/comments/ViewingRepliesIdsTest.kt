package com.shizq.bika.ui.comicinfo.comments

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
 *
 * 拆出 [CommentsViewModel] 后噪声源变了但没消失：原先是漫画点赞、收藏，
 * 现在是每一次击键（草稿进了状态），仍然需要 distinctUntilChanged。
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

    private fun state(
        viewingReplies: Comment? = null,
        composer: Composer = Composer.Closed,
        pinned: List<Comment> = emptyList(),
    ) = CommentsState(
        composer = composer,
        viewingReplies = viewingReplies,
        pinned = pinned,
    )

    @Test
    fun `初始状态投影为 null`() = runTest {
        val result = flowOf(state()).viewingRepliesIds().toList()

        assertEquals(listOf(null), result)
    }

    @Test
    fun `展开回复后发射对应 id`() = runTest {
        val result = flowOf(
            state(),
            state(viewingReplies = comment("c1")),
        ).viewingRepliesIds().toList()

        assertEquals(listOf(null, "c1"), result)
    }

    @Test
    fun `关闭回复后回到 null`() = runTest {
        val result = flowOf(
            state(viewingReplies = comment("c1")),
            state(viewingReplies = null),
        ).viewingRepliesIds().toList()

        // 原实现用 filterNotNull，这里会只剩 "c1"，
        // 下游永远回不到"没有回复在看"的状态、Pager 一直挂着
        assertEquals(listOf("c1", null), result)
    }

    @Test
    fun `草稿击键不触发回复列表重建`() = runTest {
        val target = comment("c1")
        val replyComposer = Composer.Reply(
            target = ReplyTarget(
                rootCommentId = "c1",
                targetCommentId = "c1",
                targetUserName = "用户",
            )
        )
        val result = flowOf(
            state(viewingReplies = target, composer = replyComposer.copy(draft = "")),
            state(viewingReplies = target, composer = replyComposer.copy(draft = "你")),
            state(viewingReplies = target, composer = replyComposer.copy(draft = "你好")),
        ).viewingRepliesIds().toList()

        // 每次击键一次 dispatch、一次新 state。缺 distinctUntilChanged
        // 的话在回复弹窗里打字会把回复列表逐字拉回第一页
        assertEquals(listOf("c1"), result)
    }

    @Test
    fun `置顶评论到达不触发回复列表重建`() = runTest {
        val target = comment("c1")
        val result = flowOf(
            state(viewingReplies = target),
            state(viewingReplies = target, pinned = listOf(comment("p1"))),
        ).viewingRepliesIds().toList()

        assertEquals(listOf("c1"), result)
    }

    @Test
    fun `切换到另一条评论时发射新 id`() = runTest {
        val result = flowOf(
            state(viewingReplies = comment("c1")),
            state(viewingReplies = comment("c2")),
        ).viewingRepliesIds().toList()

        assertEquals(listOf("c1", "c2"), result)
    }

    @Test
    fun `相同 id 的不同 Comment 实例不触发重建`() = runTest {
        // 评论列表刷新后 Comment 实例会变（点赞数更新等），
        // 但只要还在看同一条评论，Pager 就不该重建
        val result = flowOf(
            state(viewingReplies = comment("c1")),
            state(viewingReplies = comment("c1")),
        ).viewingRepliesIds().toList()

        assertEquals(listOf("c1"), result)
    }

    @Test
    fun `关闭后重新打开同一条会重新发射`() = runTest {
        val result = flowOf(
            state(viewingReplies = comment("c1")),
            state(viewingReplies = null),
            state(viewingReplies = comment("c1")),
        ).viewingRepliesIds().toList()

        // 中间经过 null，所以第二次打开是一次真实的状态变化，
        // 需要重建 Pager 以拉取最新回复
        assertEquals(listOf("c1", null, "c1"), result)
    }
}
