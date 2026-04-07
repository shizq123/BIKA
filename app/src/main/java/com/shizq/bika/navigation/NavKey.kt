package com.shizq.bika.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Root : NavKey

sealed interface Authentication : NavKey

@Serializable
data object AuthenticationRoute : Root {
    @Serializable
    data object LoginRoute : Authentication

    @Serializable
    data object RegisterRoute : Authentication
}

sealed interface Connected : NavKey

@Serializable
data object ConnectedRoute : Root {
    @Serializable
    data object DashboardRoute : Connected

    @Serializable
    data class FeedRoute(val action: DiscoveryAction) : Connected

    @Serializable
    data object HistoryRoute : Connected

    @Serializable
    data object LeaderboardRoute : Connected

    @Serializable
    data object MineCommentRoute : Connected

    @Serializable
    data class ReaderRoute(val id: String, val order: Int) : Connected

    @Serializable
    data object SearchRoute : Connected

    @Serializable
    data object SettingsRoute : Connected

    @Serializable
    data class UnitedDetailRoute(val id: String) : Connected
}

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
    data class AdvancedSearch(override val name: String) : DiscoveryAction

    @Serializable
    data class Channel(override val name: String) : DiscoveryAction
}
@Serializable
object GameNavKey : NavKey

@Serializable
data class GameDetailNavKey(val id: String) : NavKey

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