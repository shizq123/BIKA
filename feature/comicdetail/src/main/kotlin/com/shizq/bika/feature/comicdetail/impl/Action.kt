package com.shizq.bika.feature.comicdetail.impl

sealed interface UnitedDetailsAction {
    data object ToggleLike : UnitedDetailsAction
    data object ToggleFavorite : UnitedDetailsAction

    data object Retry : UnitedDetailsAction
}
