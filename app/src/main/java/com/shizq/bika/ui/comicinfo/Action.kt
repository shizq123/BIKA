package com.shizq.bika.ui.comicinfo

sealed interface UnitedDetailsAction {
    data object ToggleLike : UnitedDetailsAction
    data object ToggleFavorite : UnitedDetailsAction
    data class ExpandReplies(val id: String) : UnitedDetailsAction
    data object CollapseReplies : UnitedDetailsAction
    data object Retry : UnitedDetailsAction
}