package com.shizq.bika.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 认证图内的路由。用户尚未持有可用 token 时所处的图。
 */
sealed interface Authentication : NavKey

/**
 * 认证图的命名空间。
 *
 * 注意：它本身不是可导航的目标。当前处于哪个图由会话状态（token 是否存在）
 * 决定，不由导航栈表达——参见 [Navigator] 的类注释。
 */
data object AuthenticationRoute {
    @Serializable
    data object LoginRoute : Authentication

    @Serializable
    data object RegisterRoute : Authentication
}

/**
 * 主图内的路由。用户持有可用 token 时所处的图。
 */
sealed interface Connected : NavKey

/**
 * 主图的命名空间。与 [AuthenticationRoute] 同理，本身不可导航。
 */
data object ConnectedRoute {
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
    data class ReaderRoute(val id: String, val order: Int, val downloadedOnly: Boolean = false) : Connected

    @Serializable
    data object SearchRoute : Connected

    @Serializable
    data object SettingsRoute : Connected

    @Serializable
    data object DownloadListRoute : Connected

    @Serializable
    data class UnitedDetailRoute(val id: String) : Connected

    @Serializable
    data object NotificationsRoute : Connected

    @Serializable
    data object GuestbookRoute : Connected

    @Serializable
    data object StorageManagerRoute : Connected

    @Serializable
    data object DnsSettingsRoute : Connected

    @Serializable
    data object BlockedTagsRoute : Connected
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
internal data class SearchKey(
    val topic: String? = null,
    val tag: String? = null,
    val authorName: String? = null,
    val knightId: String? = null,
    val translationTeam: String? = null,
) : NavKey {
//    override val name: String = STRING_LITERAL_SEARCH
}