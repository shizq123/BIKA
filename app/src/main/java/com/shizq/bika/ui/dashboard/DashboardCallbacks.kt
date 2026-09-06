package com.shizq.bika.ui.dashboard

import com.shizq.bika.navigation.DiscoveryAction

/**
 * Dashboard 向外的导航出口。
 */
data class DashboardCallbacks(
    val navigateToLeaderboard: () -> Unit,
    val navigateToFavourite: (DiscoveryAction) -> Unit,
    val navigateToHistory: () -> Unit,
    val navigateToSettings: () -> Unit,
    val navigateToReader: (comicId: String, chapterOrder: Int) -> Unit,
    val onSearchClick: () -> Unit,
    val onChannelPreferenceClick: () -> Unit,
    val onCommentsClick: () -> Unit,
    val onDownloadsClick: () -> Unit,
    val onNotificationsClick: () -> Unit,
    val onBlockedTagsClick: () -> Unit,
)
