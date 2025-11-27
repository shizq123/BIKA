package com.shizq.bika.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.shizq.bika.R
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
sealed interface DashboardEntry {
    data class Native(
        @StringRes val titleResId: Int,
        @DrawableRes val imageResId: Int,
        val type: EntryType
    ) : DashboardEntry

    data class Remote(
        val title: String,
        val imageUrl: String,
        val id: String = Uuid.random().toString(),
        val isWeb: Boolean = false,
        val link: String? = null
    ) : DashboardEntry
}

val dashboardEntries = listOf(
    DashboardEntry.Native(R.string.categories_recommend, R.drawable.ic_bika, EntryType.RECOMMEND),
    DashboardEntry.Native(
        R.string.categories_leaderboard,
        R.drawable.ic_cat_ranking,
        EntryType.LEADERBOARD
    ),
    DashboardEntry.Native(R.string.categories_game, R.drawable.ic_cat_game_rec, EntryType.GAME),
    DashboardEntry.Native(R.string.categories_apps, R.drawable.ic_cat_mini_app, EntryType.APPS),
    DashboardEntry.Native(
        R.string.categories_forum,
        R.drawable.ic_cat_message_board,
        EntryType.FORUM
    ),
    DashboardEntry.Native(R.string.categories_latest, R.drawable.ic_cat_recent, EntryType.LATEST),
    DashboardEntry.Native(R.string.categories_random, R.drawable.ic_cat_random, EntryType.RANDOM),
)

enum class EntryType {
    RECOMMEND,    // 哔咔推荐
    LEADERBOARD,  // 排行榜
    GAME,         // 游戏
    APPS,         // 应用
    CHAT,         // 聊天室
    FORUM,        // 论坛
    LATEST,       // 最新
    RANDOM        // 随机
}