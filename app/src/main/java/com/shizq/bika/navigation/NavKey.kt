package com.shizq.bika.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object DashboardNavKey : NavKey

//@Serializable
sealed interface DiscoveryAction {
    val name: String

    object ToCollections : DiscoveryAction {
        override val name: String = "本子妹推薦"
    }

    object ToRecent : DiscoveryAction {
        override val name: String = "最近更新"
    }

    object ToRandom : DiscoveryAction {
        override val name: String = "随机本子"
    }

    data class Knight(override val name: String, val id: String) : DiscoveryAction

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
internal data class SearchKey(
    val topic: String? = null,
    val tag: String? = null,
    val authorName: String? = null,
    val knightId: String? = null,
    val translationTeam: String? = null,
) : NavKey {
//    override val name: String = STRING_LITERAL_SEARCH
}