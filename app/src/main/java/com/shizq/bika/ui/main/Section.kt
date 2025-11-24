package com.shizq.bika.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.shizq.bika.R
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
sealed interface Section {
    data class Local(@StringRes val titleResId: Int, @DrawableRes val imageResId: Int) : Section
    data class Remote(
        val title: String,
        val imageUrl: String,
        val id: String = Uuid.random().toString(),
        val isWeb: Boolean = false,
        val link: String? = null
    ) : Section
}

val staticSections = listOf(
    Section.Local(R.string.categories_recommend, R.drawable.bika),
    Section.Local(R.string.categories_leaderboard, R.drawable.cat_leaderboard),
    Section.Local(R.string.categories_game, R.drawable.cat_game),
    Section.Local(R.string.categories_apps, R.drawable.cat_love_pica),
    Section.Local(R.string.categories_forum, R.drawable.cat_forum),
    Section.Local(R.string.categories_latest, R.drawable.cat_latest),
    Section.Local(R.string.categories_random, R.drawable.cat_random),
)