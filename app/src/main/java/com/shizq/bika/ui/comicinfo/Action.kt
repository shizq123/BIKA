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

    data object Retry : UnitedDetailsAction
}
