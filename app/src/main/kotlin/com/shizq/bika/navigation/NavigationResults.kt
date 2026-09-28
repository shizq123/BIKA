package com.shizq.bika.navigation

import com.shizq.bika.core.model.FavoriteTag

/** Transient results exchanged between a dialog entry and the entry below it. */
data class FeedPageJumpResult(val page: Int)
data class AddFavoriteTagResult(val name: String)
data class RenameFavoriteTagResult(
    val tag: FavoriteTag,
    val name: String,
)
