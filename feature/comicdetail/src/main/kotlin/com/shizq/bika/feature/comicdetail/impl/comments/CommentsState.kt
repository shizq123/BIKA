package com.shizq.bika.feature.comicdetail.impl.comments

import androidx.compose.runtime.Immutable
import com.shizq.bika.core.data.model.Comment

/**
 * 评论域的完整状态。
 *
 * 只有一个态（没有 Initialize / Error）：评论页在漫画详情加载完成后才可见，
 * 进入时已经确定有 comicId；列表自身的加载与错误由 Paging 呈现，置顶评论
 * 失败则保留上一次的值。少一个 Initialize 态顺带解决了原先
 * [com.shizq.bika.ui.comicinfo.statemachine.UnitedDetailsStateMachine] 上
 * "Initialize 期间 dispatch 的 action 会被丢弃"的问题——那正是置顶评论
 * 当初被搬出状态机的原因。
 */
@Immutable
data class CommentsState(
    val composer: Composer = Composer.Closed,
    /**
     * 正在查看回复的根评论，null 表示回复弹窗关闭。
     *
     * 存整个 Comment 而不是 id：弹窗要展示根评论本身（作者、内容、点赞数），
     * 而根评论未必在已加载的页里——从回复中再进一层时就查不到。
     *
     * 注意向 UI 暴露前要过一遍 [likeOverrides]，否则弹窗顶部的爱心点了不动。
     */
    val viewingReplies: Comment? = null,
    /**
     * 评论点赞的本地覆盖层：commentId -> 用户切换后的状态。
     *
     * PagingData 不可变，点赞结果没法写回已加载的页，所以用一层覆盖在读取时合并。
     * 只记录"用户改成了什么"，不记录原始值——单靠覆盖层无法区分
     * "未操作过的已点赞"和"未操作过的未点赞"，所以 ToggleLike 必须由调用方
     * 传入列表当前显示的状态。
     *
     * 与 [viewingReplies]、[pinned] 同住一个状态：三者原先分居 ViewModel 字段、
     * 详情状态机、独立 StateFlow 三处，覆盖层改了而根评论不知道，弹窗里
     * 给根评论点赞要关掉重开才生效。
     */
    val likeOverrides: Map<String, Boolean> = emptyMap(),
    /**
     * 置顶评论。不分页、与页码无关，所以不走 PagingSource。
     *
     * 与列表第一页共用 [com.shizq.bika.core.data.repository.CommentsRepository]，
     * 两者本是同一个接口响应的两半，并发请求在数据层合流成一次网络往返。
     */
    val pinned: List<Comment> = emptyList(),
    /**
     * 发表成功后自增，UI 据此调 `LazyPagingItems.refresh()`。
     *
     * 用 token 而不是让状态机直接刷新：refresh() 只能从 UI 侧调用，
     * 状态机碰不到 LazyPagingItems。这是 Paging 与 redux 的边界摩擦。
     */
    val listRefreshToken: Int = 0,
)

/**
 * 一次回复操作的目标。
 *
 * [rootCommentId] 是服务端 `postCommentReply` 接受的根评论 id；
 * [targetCommentId] 是用户实际点击的评论，当前仅用于保留准确的交互语义；
 * [targetUserName] 用于输入框提示，不参与请求。
 */
@Immutable
data class ReplyTarget(
    val rootCommentId: String,
    val targetCommentId: String,
    val targetUserName: String,
)

/** 评论输入器。草稿活在状态里，所以切 tab、转屏都不丢，发送失败也原样保留。 */
sealed interface Composer {
    data object Closed : Composer

    sealed interface Open : Composer {
        val draft: String
        val sending: Boolean
        val error: Throwable?
    }

    data class MainComment(
        override val draft: String = "",
        override val sending: Boolean = false,
        override val error: Throwable? = null,
    ) : Open

    data class Reply(
        val target: ReplyTarget,
        override val draft: String = "",
        override val sending: Boolean = false,
        override val error: Throwable? = null,
    ) : Open
}
