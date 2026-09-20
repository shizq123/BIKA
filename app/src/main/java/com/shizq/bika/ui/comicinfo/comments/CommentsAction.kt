package com.shizq.bika.ui.comicinfo.comments

import com.shizq.bika.core.data.model.Comment

sealed interface CommentsAction {

    /**
     * 打开输入器。
     *
     * @param replyToId null 表示发主评论；否则是被回复的根评论 id
     */
    data class OpenComposer(
        val replyToId: String? = null,
        val replyToName: String? = null,
    ) : CommentsAction

    /**
     * 草稿变更。每次击键一次 dispatch。
     *
     * 草稿进状态而不是留在 Composable 的 remember 里：这样"成功后清空、
     * 失败后保留"都是普通状态转移，不需要一次性事件通道（flowredux2 没有）。
     * 代价是按键路径上多了一次 channel 派发，真出现 IME 延迟的话退路是在
     * TextField 侧加本地镜像。
     */
    data class DraftChanged(val text: String) : CommentsAction

    data object Send : CommentsAction

    data object DismissComposer : CommentsAction

    /**
     * 切换评论点赞。
     *
     * @param currentlyLiked 列表当前显示的点赞状态。必须由调用方传入：
     *   真实状态在 PagingData 里，覆盖层只记录用户改成了什么，
     *   见 [CommentsState.likeOverrides]
     */
    data class ToggleLike(
        val commentId: String,
        val currentlyLiked: Boolean,
    ) : CommentsAction

    /** 携带整个 Comment 而不是 id，理由见 [CommentsState.viewingReplies] */
    data class ExpandReplies(val comment: Comment) : CommentsAction

    data object CollapseReplies : CommentsAction

    data object RefreshPinned : CommentsAction
}
