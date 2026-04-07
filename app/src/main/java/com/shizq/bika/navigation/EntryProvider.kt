package com.shizq.bika.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.shizq.bika.ui.comicinfo.ComicDetailScreen
import com.shizq.bika.ui.comicinfo.ComicInfoViewModel
import com.shizq.bika.ui.comment.mine.MineCommentScreen
import com.shizq.bika.ui.dashboard.ChannelSettingsDialog
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

fun EntryProviderScope<NavKey>.rootSection(
    navigator: Navigator,
) {
    entry<AuthenticationRoute> {
        NavDisplay(
            backStack = navigator.state.authenticationBackStack,
            entryProvider = entryProvider {
                authenticationSection(
                    navigationToDashboard = { navigator.navigate(ConnectedRoute) },
                    navigateToRegister = {
                        navigator.navigate(AuthenticationRoute.RegisterRoute)
                    },
                    onBackClick = navigator::goBack,
                )
            }
        )
    }

    entry<ConnectedRoute> {
        val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
        NavDisplay(
            entries = navigator.state.toEntries(
                entryProvider = entryProvider {
                    featureSection(navigator)
                }
            ),
            onBack = navigator::goBack,
            sceneStrategies = listOf(dialogStrategy)
        )
    }
}

fun EntryProviderScope<NavKey>.authenticationSection(
    navigationToDashboard: () -> Unit,
    navigateToRegister: () -> Unit,
    onBackClick: () -> Unit,
) {
    entry<AuthenticationRoute.LoginRoute> {
        LoginScreen(
            onNavigateToDashboard = navigationToDashboard,
            onNavigateToSignUp = navigateToRegister,
            onNavigateToForgotPassword = {}
        )
    }
    entry<AuthenticationRoute.RegisterRoute> {
        RegistrationScreen(
            onBackClick = onBackClick
        )
    }
}

fun EntryProviderScope<NavKey>.featureSection(
    navigator: Navigator,
) {
    entry<ConnectedRoute.DashboardRoute> {
        DashboardScreen(
            navigationToLeaderboard = { navigator.navigate(ConnectedRoute.LeaderboardRoute) },
            navigateToFavourite = { action ->
                navigator.navigate(ConnectedRoute.FeedRoute(action))
            },
            navigationToHistory = { navigator.navigate(ConnectedRoute.HistoryRoute) },
            navigationToSettings = { navigator.navigate(ConnectedRoute.SettingsRoute) },
            navigateToGame = { /*navigator.navigate(GameNavKey)*/ },
            onSearchClick = { navigator.navigate(ConnectedRoute.SearchRoute) },
            onChannelPreferenceClick = { navigator.navigate(ChannelSettingsNavKey) },
            onCommentsClick = { navigator.navigate(ConnectedRoute.MineCommentRoute) },
        )
    }
    entry<ConnectedRoute.FeedRoute> { key ->
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
    entry<ConnectedRoute.HistoryRoute> {
        HistoryScreen(
            onComicClick = navigator::navigateToUnitedDetail,
            onBackClick = navigator::goBack
        )
    }
    entry<ConnectedRoute.LeaderboardRoute> {
        LeaderboardScreen(
            navigationToUnitedDetail = { navigator.navigateToUnitedDetail(it) },
            navigationToKnight = { name, id ->
                navigator.navigate(ConnectedRoute.FeedRoute(DiscoveryAction.Knight(name, id)))
            }
        )
    }
    entry<ConnectedRoute.MineCommentRoute> { key ->
        MineCommentScreen(
            onCardClick = navigator::navigateToUnitedDetail,
            onBackClick = navigator::goBack
        )
    }
    entry<ConnectedRoute.ReaderRoute> { key ->
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

    entry<ConnectedRoute.SearchRoute>(
        metadata = metadata {
            put(NavDisplay.TransitionKey) {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) togetherWith ExitTransition.KeepUntilTransitionsFinished
            }

            put(NavDisplay.PopTransitionKey) {
                EnterTransition.None togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(300)
                        )
            }

            put(NavDisplay.PredictivePopTransitionKey) {
                EnterTransition.None togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(300)
                        )
            }
        }
    ) {
        SearchScreen(
            onSearchClick = {
                navigator.navigate(ConnectedRoute.FeedRoute(DiscoveryAction.AdvancedSearch(it)))
            },
            onBackClick = navigator::goBack
        )
    }
    entry<ConnectedRoute.SettingsRoute> {
        SettingsScreen(
            navigationToLogin = { navigator.navigate(AuthenticationRoute) },
            onBackClick = navigator::goBack
        )
    }
    entry<ConnectedRoute.UnitedDetailRoute> { key ->
        val id = key.id
        ComicDetailScreen(
            viewModel = hiltViewModel<ComicInfoViewModel, ComicInfoViewModel.Factory>(
                key = id,
            ) { factory ->
                factory.create(id)
            },
            onBackClick = { navigator.goBack() },
            navigationToReader = { id, index ->
                navigator.navigate(ConnectedRoute.ReaderRoute(id, index))
            },
            onForYouClick = { navigator.navigateToUnitedDetail(it) },
            navigationToFeed = { action ->
                navigator.navigate(ConnectedRoute.FeedRoute(action))
            }
        )
    }

    entry<ChannelSettingsNavKey>(
        metadata = DialogSceneStrategy.dialog(),
    ) {
        ChannelSettingsDialog(
            onDismiss = navigator::goBack,
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
    navigate(ConnectedRoute.UnitedDetailRoute(id))
}