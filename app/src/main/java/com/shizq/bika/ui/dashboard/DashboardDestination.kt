package com.shizq.bika.ui.dashboard

import com.shizq.bika.navigation.DiscoveryAction

/**
 * Dashboard 能去的地方。
 *
 * 取代原先 12 个独立回调参数（[DashboardCallbacks]）：那种形状要求屏幕入参列表与
 * 组装处的 `remember` key 列表手工保持一致，漏一个就拿到过期 lambda——不崩不报错，
 * 只是点了没反应。收敛成单个意图后，导航层只需 `remember(navigator)`，
 * 且新增目标会让导航层的 `when` 编译失败，而新增参数只会静默漏接线。
 *
 * 与 [ChannelDestination] 同一思路：让「去哪」成为可测的返回值，而不是让 UI
 * 直接持有导航能力。这里刻意不用 `(NavKey) -> Unit`——那等于把整个路由表交给屏幕，
 * 任何一屏都能构造与自己无关的路由。
 */
sealed interface DashboardDestination {
    data object Leaderboard : DashboardDestination

    /** 内容流，[action] 决定查询哪一类内容。 */
    data class Feed(val action: DiscoveryAction) : DashboardDestination

    data object History : DashboardDestination

    data object Settings : DashboardDestination

    data class Reader(val comicId: String, val chapterOrder: Int) : DashboardDestination

    data object Search : DashboardDestination

    data object ChannelPreference : DashboardDestination

    /** [initialSlogan] 是打开那一刻的签名，随 NavKey 进返回栈，见 EditProfileNavKey。 */
    data class EditProfile(val initialSlogan: String) : DashboardDestination

    data object Comments : DashboardDestination

    data object Downloads : DashboardDestination

    data object Notifications : DashboardDestination

    data object BlockedTags : DashboardDestination
}
