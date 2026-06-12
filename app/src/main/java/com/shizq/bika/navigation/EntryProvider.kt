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

import com.shizq.bika.ui.history.HistoryScreen
import com.shizq.bika.ui.leaderboard.LeaderboardScreen
import com.shizq.bika.ui.reader.ReaderScreen
import com.shizq.bika.ui.reader.ReaderViewModel
import com.shizq.bika.ui.search.SearchScreen
import com.shizq.bika.ui.settings.SettingsScreen
import com.shizq.bika.ui.settings.StorageManagerScreen
import com.shizq.bika.ui.signin.LoginScreen
import com.shizq.bika.ui.signup.RegistrationScreen
import com.shizq.bika.ui.download.DownloadListScreen
import com.shizq.bika.ui.notifications.NotificationsScreen

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

private fun slideTransitionMetadata() = metadata {
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

            onSearchClick = { navigator.navigate(ConnectedRoute.SearchRoute) },
            onChannelPreferenceClick = { navigator.navigate(ChannelSettingsNavKey) },
            onCommentsClick = { navigator.navigate(ConnectedRoute.MineCommentRoute) },
            onDownloadsClick = { navigator.navigate(ConnectedRoute.DownloadListRoute) },
            onNotificationsClick = { navigator.navigate(ConnectedRoute.NotificationsRoute) },
            navigationToReader = { id, order ->
                navigator.navigate(ConnectedRoute.ReaderRoute(id, order))
            },
        )
    }


    entry<ConnectedRoute.FeedRoute>(
        metadata = slideTransitionMetadata()
    ) { key ->
        FeedScreen(
            title = key.action.name,
            onBackClick = { navigator.goBack() },
            onComicClick = navigator::navigateToUnitedDetail,
            onNavigateToFeed = { action ->
                navigator.navigate(ConnectedRoute.FeedRoute(action))
            },
            viewModel = hiltViewModel<FeedViewModel, FeedViewModel.Factory>(
                key = key.toString(),
            ) { factory ->
                factory.create(key.action)
            },
        )
    }
    entry<ConnectedRoute.HistoryRoute>(
        metadata = slideTransitionMetadata()
    ) {
        HistoryScreen(
            onComicClick = navigator::navigateToUnitedDetail,
            onBackClick = navigator::goBack,
            onReadLatestClick = { id, order ->
                navigator.navigate(ConnectedRoute.ReaderRoute(id, order))
            }
        )
    }
    entry<ConnectedRoute.LeaderboardRoute>(
        metadata = slideTransitionMetadata()
    ) {
        LeaderboardScreen(
            navigationToUnitedDetail = { navigator.navigateToUnitedDetail(it) },
            navigationToKnight = { name, id ->
                navigator.navigate(ConnectedRoute.FeedRoute(DiscoveryAction.Knight(name, id)))
            }
        )
    }
    entry<ConnectedRoute.MineCommentRoute>(
        metadata = slideTransitionMetadata()
    ) { key ->
        MineCommentScreen(
            onCardClick = navigator::navigateToUnitedDetail,
            onBackClick = navigator::goBack
        )
    }
    entry<ConnectedRoute.ReaderRoute>(
        metadata = slideTransitionMetadata()
    ) { key ->
        val id = key.id
        ReaderScreen(
            onBackClick = { navigator.goBack() },
            viewModel = hiltViewModel<ReaderViewModel, ReaderViewModel.Factory>(
                key = key.toString(),
            ) { factory ->
                factory.create(id, key.order, key.downloadedOnly)
            },
        )
    }

    entry<ConnectedRoute.SearchRoute>(
        metadata = slideTransitionMetadata()
    ) {
        SearchScreen(
            onSearchClick = {
                navigator.navigate(ConnectedRoute.FeedRoute(DiscoveryAction.AdvancedSearch(it)))
            },
            onBackClick = navigator::goBack
        )
    }
    entry<ConnectedRoute.SettingsRoute>(
        metadata = slideTransitionMetadata()
    ) {
        SettingsScreen(
            navigationToLogin = { navigator.navigate(AuthenticationRoute) },
            navigationToStorageManager = { navigator.navigate(ConnectedRoute.StorageManagerRoute) },
            onBackClick = navigator::goBack
        )
    }
    entry<ConnectedRoute.StorageManagerRoute>(
        metadata = slideTransitionMetadata()
    ) {
        StorageManagerScreen(
            onBackClick = navigator::goBack
        )
    }
    entry<ConnectedRoute.DownloadListRoute>(
        metadata = slideTransitionMetadata()
    ) {
        DownloadListScreen(
            onBackClick = navigator::goBack,
            onComicClick = { comicId, order ->
                // 跳转到下载阅读器（仅限已下载章节导航）
                navigator.navigate(ConnectedRoute.ReaderRoute(comicId, order, downloadedOnly = true))
            }
        )
    }
    entry<ConnectedRoute.UnitedDetailRoute>(
        metadata = slideTransitionMetadata()
    ) { key ->
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
    entry<ConnectedRoute.NotificationsRoute>(
        metadata = slideTransitionMetadata()
    ) {
        NotificationsScreen(
            onComicClick = navigator::navigateToUnitedDetail,
            onBackClick = navigator::goBack
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



fun Navigator.navigateToUnitedDetail(id: String) {
    navigate(ConnectedRoute.UnitedDetailRoute(id))
}