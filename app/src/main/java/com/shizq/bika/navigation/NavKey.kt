package com.shizq.bika.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object DashboardNavKey : NavKey

@Serializable
sealed interface DiscoveryAction {
    val name: String

    @Serializable
    object ToCollections : DiscoveryAction {
        override val name: String = "本子妹推薦"
    }

    @Serializable
    object ToRecent : DiscoveryAction {
        override val name: String = "最近更新"
    }

    @Serializable
    object ToRandom : DiscoveryAction {
        override val name: String = "随机本子"
    }

    @Serializable
    object ToFavourite : DiscoveryAction {
        override val name: String = "我的收藏"
    }

    @Serializable
    data class Knight(override val name: String, val id: String) : DiscoveryAction

    @Serializable
    data class AdvancedSearch(
        override val name: String,
        val topic: String? = null,
        val tag: String? = null,
        val authorName: String? = null,
        val knightId: String? = null,
        val translationTeam: String? = null
    ) : DiscoveryAction
}

@Serializable
data class FeedNavKey(val action: DiscoveryAction) : NavKey

@Serializable
object LeaderboardNavKey : NavKey

@Serializable
data class UnitedDetailNavKey(val id: String) : NavKey

@Serializable
data class ReaderNavKey(val id: String, val order: Int) : NavKey

@Serializable
object HistoryNavKey : NavKey

@Serializable
internal data class SearchKey(
    val topic: String? = null,
    val tag: String? = null,
    val authorName: String? = null,
    val knightId: String? = null,
    val translationTeam: String? = null,
) : NavKey {
//    override val name: String = STRING_LITERAL_SEARCH
}