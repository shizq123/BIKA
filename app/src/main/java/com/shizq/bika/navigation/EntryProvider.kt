package com.shizq.bika.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.shizq.bika.ui.comicinfo.ComicDetailScreen
import com.shizq.bika.ui.comicinfo.ComicInfoViewModel
import com.shizq.bika.ui.dashboard.DashboardScreen
import com.shizq.bika.ui.feed.FeedScreen
import com.shizq.bika.ui.feed.FeedViewModel
import com.shizq.bika.ui.leaderboard.LeaderboardScreen

fun EntryProviderScope<NavKey>.dashboardEntry(navigator: Navigator) {
    entry<DashboardNavKey> {
        DashboardScreen(
            navigationToCollections = { navigator.navigate(FeedNavKey.Collection) },
            navigationToRandom = { navigator.navigate(FeedNavKey.Random) },
            navigationToTopic = { navigator.navigate(FeedNavKey.Topic(it)) },
            navigationToRecent = { navigator.navigate(FeedNavKey.Recent) },
            navigationToLeaderboard = { navigator.navigate(LeaderboardNavKey) },
        )
    }
}

fun EntryProviderScope<NavKey>.leaderboardEntry(navigator: Navigator) {
    entry<LeaderboardNavKey> {
        LeaderboardScreen(
            navigationToUnitedDetail = { navigator.navigate(UnitedDetailNavKey(it)) },
            navigationToKnight = { name, id -> navigator.navigate(FeedNavKey.Knight(name, id)) })
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
        )
    }
}

fun EntryProviderScope<NavKey>.feedEntry(navigator: Navigator) {
    addEntryProvider<FeedNavKey.Collection>(navigator)
    addEntryProvider<FeedNavKey.Random>(navigator)
    addEntryProvider<FeedNavKey.Topic>(navigator)
    addEntryProvider<FeedNavKey.Recent>(navigator)
    addEntryProvider<FeedNavKey.Knight>(navigator)
}

fun Navigator.navigateToUnitedDetail(id: String) {
    navigate(UnitedDetailNavKey(id))
}

inline fun <reified T> EntryProviderScope<NavKey>.addEntryProvider(navigator: Navigator) where T : NavKey, T : FeedNavKey {
    entry<T> { key ->
        FeedScreen(
            onBackClick = { navigator.goBack() },
            navigationToComicDetail = navigator::navigateToUnitedDetail,
            viewModel = hiltViewModel<FeedViewModel, FeedViewModel.Factory>(
                key = key.toString(),
            ) { factory ->
                factory.create(key)
            },
        )
    }
}