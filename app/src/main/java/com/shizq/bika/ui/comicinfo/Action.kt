package com.shizq.bika.ui.comicinfo

sealed interface UnitedDetailsAction {
    data object ToggleLike : UnitedDetailsAction
    data object ToggleFavorite : UnitedDetailsAction
}