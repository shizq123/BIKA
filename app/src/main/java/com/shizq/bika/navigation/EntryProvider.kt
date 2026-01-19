package com.shizq.bika.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.shizq.bika.ui.comicinfo.ComicDetailScreen
import com.shizq.bika.ui.comicinfo.ComicInfoViewModel
import com.shizq.bika.ui.dashboard.DashboardScreen
import com.shizq.bika.ui.feed.FeedScreen
import com.shizq.bika.ui.feed.FeedViewModel
import com.shizq.bika.ui.history.HistoryScreen
import com.shizq.bika.ui.leaderboard.LeaderboardScreen
import com.shizq.bika.ui.reader.ReaderScreen
import com.shizq.bika.ui.reader.ReaderViewModel
import com.shizq.bika.ui.settings.SettingsScreen
import com.shizq.bika.ui.signin.LoginScreen

fun EntryProviderScope<NavKey>.loginEntry(navigator: Navigator) {
    entry<LoginNavKey> {
        LoginScreen(
            onLoginSuccess = { navigator.navigate(DashboardNavKey) },
            onSignUpClick = {},
            onForgotPasswordClick = {}
        )
    }
}

fun EntryProviderScope<NavKey>.dashboardEntry(navigator: Navigator) {
    entry<DashboardNavKey> {
        DashboardScreen(
            navigationToLeaderboard = { navigator.navigate(LeaderboardNavKey) },
            navigateToSearch = { action ->
                navigator.navigate(FeedNavKey(action))
            },
            navigationToHistory = { navigator.navigate(HistoryNavKey) },
            navigationToSettings = { navigator.navigate(SettingsNavKey) },
        )
    }
}

fun EntryProviderScope<NavKey>.leaderboardEntry(navigator: Navigator) {
    entry<LeaderboardNavKey> {
        LeaderboardScreen(
            navigationToUnitedDetail = { navigator.navigateToUnitedDetail(it) },
            navigationToKnight = { name, id ->
                navigator.navigate(FeedNavKey(DiscoveryAction.Knight(name, id)))
            }
        )
    }
}

fun EntryProviderScope<NavKey>.unitedDetailNavKeyEntry(navigator: Navigator) {
    entry<UnitedDetailNavKey> { key ->
        val id = key.id
        ComicDetailScreen(
            viewModel = hiltViewModel<ComicInfoViewModel, ComicInfoViewModel.Factory>(
                key = id,
            ) { factory ->
                factory.create(id)
            },
            onBackClick = { navigator.goBack() },
            navigationToReader = { id, index -> navigator.navigate(ReaderNavKey(id, index)) },
            onForYouClick = { navigator.navigateToUnitedDetail(it) },
            navigationToFeed = { action ->
                navigator.navigate(FeedNavKey(action))
            }
        )
    }
}

fun EntryProviderScope<NavKey>.readerNavKeyEntry(navigator: Navigator) {
    entry<ReaderNavKey> { key ->
        val id = key.id
        ReaderScreen(
            onBackClick = { navigator.goBack() },
            viewModel = hiltViewModel<ReaderViewModel, ReaderViewModel.Factory>(
                key = id,
            ) { factory ->
                factory.create(id, key.order)
            },
        )
    }
}

fun EntryProviderScope<NavKey>.feedEntry(navigator: Navigator) {
    entry<FeedNavKey> { key ->
        FeedScreen(
            title = key.action.name,
            onBackClick = { navigator.goBack() },
            navigationToComicDetail = navigator::navigateToUnitedDetail,
            viewModel = hiltViewModel<FeedViewModel, FeedViewModel.Factory>(
                key = key.toString(),
            ) { factory ->
                factory.create(key.action)
            },
        )
    }
}

fun EntryProviderScope<NavKey>.historyEntry(navigator: Navigator) {
    entry<HistoryNavKey> {
        HistoryScreen(
            onComicClick = navigator::navigateToUnitedDetail,
            onBackClick = navigator::goBack
        )
    }
}

fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<SettingsNavKey> {
        SettingsScreen(
            onLogoutClicked = { },
            onBackClick = navigator::goBack
        )
    }
}

fun Navigator.navigateToUnitedDetail(id: String) {
    navigate(UnitedDetailNavKey(id))
}