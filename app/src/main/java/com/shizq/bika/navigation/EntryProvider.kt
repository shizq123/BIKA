package com.shizq.bika.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.shizq.bika.ui.comicinfo.ComicDetailScreen
import com.shizq.bika.ui.comicinfo.ComicInfoViewModel
import com.shizq.bika.ui.dashboard.DashboardScreen
import com.shizq.bika.ui.feed.FeedScreen
import com.shizq.bika.ui.feed.FeedViewModel
import com.shizq.bika.ui.games.GameDetailScreen
import com.shizq.bika.ui.games.GameDetailViewModel
import com.shizq.bika.ui.games.GameScreen
import com.shizq.bika.ui.history.HistoryScreen
import com.shizq.bika.ui.leaderboard.LeaderboardScreen
import com.shizq.bika.ui.reader.ReaderScreen
import com.shizq.bika.ui.reader.ReaderViewModel
import com.shizq.bika.ui.search.SearchScreen
import com.shizq.bika.ui.settings.SettingsScreen
import com.shizq.bika.ui.signin.LoginScreen
import com.shizq.bika.ui.signup.RegistrationScreen
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.DialogSceneStrategy
import com.shizq.bika.ui.dashboard.ChannelSettingsDialog

fun EntryProviderScope<NavKey>.loginEntry(navigator: Navigator) {
    entry<LoginNavKey> {
        LoginScreen(
            onLoginSuccess = { navigator.navigate(DashboardNavKey) },
            onSignUpClick = { navigator.navigate(RegisterNavKey) },
            onForgotPasswordClick = {}
        )
    }
}

fun EntryProviderScope<NavKey>.searchEntry(navigator: Navigator) {
    entry<SearchNavKey>(
        metadata = metadata {
            put(NavDisplay.TransitionKey) {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(1000)
                ) togetherWith ExitTransition.KeepUntilTransitionsFinished
            }

            put(NavDisplay.PopTransitionKey) {
                EnterTransition.None togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(1000)
                        )
            }

            put(NavDisplay.PredictivePopTransitionKey) {
                EnterTransition.None togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(1000)
                        )
            }
        }
    ) {
        SearchScreen(
            onSearchClick = {
                navigator.navigate(FeedNavKey(DiscoveryAction.AdvancedSearch(it)))
            },
            onBackClick = navigator::goBack
        )
    }
}

fun EntryProviderScope<NavKey>.registerEntry(navigator: Navigator) {
    entry<RegisterNavKey> {
        RegistrationScreen(
            onBackClicked = navigator::goBack
        )
    }
}

fun EntryProviderScope<NavKey>.dashboardEntry(navigator: Navigator) {
    entry<DashboardNavKey> {
        DashboardScreen(
            navigationToLeaderboard = { navigator.navigate(LeaderboardNavKey) },
            navigateToFavourite = { action ->
                navigator.navigate(FeedNavKey(action))
            },
            navigationToHistory = { navigator.navigate(HistoryNavKey) },
            navigationToSettings = { navigator.navigate(SettingsNavKey) },
            navigateToGame = { navigator.navigate(GameNavKey) },
            onSearchClicked = { navigator.navigate(SearchNavKey) },
            onChannelPreferenceClicked = { navigator.navigate(ChannelSettingsNavKey) }
        )
    }
}

fun EntryProviderScope<NavKey>.channelSettingsEntry(navigator: Navigator) {
    entry<ChannelSettingsNavKey>(
        metadata = DialogSceneStrategy.dialog(),
    ) {
        ChannelSettingsDialog(
            onDismiss = navigator::goBack,
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
            onComicClick = navigator::navigateToUnitedDetail,
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
            onBackClick = navigator::goBack
        )
    }
}

fun EntryProviderScope<NavKey>.gameEntry(navigator: Navigator) {
    entry<GameNavKey> {
        GameScreen(
            navigationToGameDetail = { navigator.navigate(GameDetailNavKey(it)) },
            onBackClick = navigator::goBack
        )
    }
}

fun EntryProviderScope<NavKey>.gameDetailEntry(navigator: Navigator) {
    entry<GameDetailNavKey> { key ->
        GameDetailScreen(
            onBackClick = navigator::goBack,
            viewModel = hiltViewModel<GameDetailViewModel, GameDetailViewModel.Factory>(
                key = key.id,
            ) { factory ->
                factory.create(key.id)
            },
        )
    }
}

fun Navigator.navigateToUnitedDetail(id: String) {
    navigate(UnitedDetailNavKey(id))
}