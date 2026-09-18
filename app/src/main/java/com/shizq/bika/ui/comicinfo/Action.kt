package com.shizq.bika.ui.comicinfo

import com.shizq.bika.core.data.model.Comment

sealed interface UnitedDetailsAction {
    data object ToggleLike : UnitedDetailsAction
    data object ToggleFavorite : UnitedDetailsAction

    /**
     * 携带整个 Comment 而不是 id：回复弹窗要展示根评论本身。
     * 详见 [UnitedDetailsUiState.Content.viewingReplies]。
     */
    data class ExpandReplies(val comment: Comment) : UnitedDetailsAction
    data object CollapseReplies : UnitedDetailsAction

    /**
     * 评论第一页加载完成时带回的置顶评论。
     *
     * 走 Action 而不是让 PagingSource 直写 ViewModel 的 MutableStateFlow，
     * 是为了保证 Content 只有状态机一个写入者。
     */
    data class TopCommentsLoaded(val comments: List<Comment>) : UnitedDetailsAction

    data object Retry : UnitedDetailsAction
}
