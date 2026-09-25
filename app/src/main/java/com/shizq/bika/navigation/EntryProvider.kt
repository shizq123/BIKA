package com.shizq.bika.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.shizq.bika.feature.reader.impl.ReaderScreen
import com.shizq.bika.feature.reader.impl.ReaderViewModel
import com.shizq.bika.feature.settings.impl.BlockedTagsScreen
import com.shizq.bika.feature.settings.impl.DnsSettingsScreen
import com.shizq.bika.feature.settings.impl.SettingsScreen
import com.shizq.bika.feature.settings.impl.StorageManagerScreen
import com.shizq.bika.ui.comicinfo.ComicDetailScreen
import com.shizq.bika.ui.comicinfo.ComicInfoViewModel
import com.shizq.bika.ui.comicinfo.download.EpisodeDownloadSheet
import com.shizq.bika.ui.comicinfo.download.EpisodeDownloadViewModel
import com.shizq.bika.ui.comicinfo.tag.TagBlockDialog
import com.shizq.bika.ui.comment.mine.MineCommentScreen
import com.shizq.bika.ui.dashboard.ChangePasswordDialog
import com.shizq.bika.ui.dashboard.ChannelSettingsDialog
import com.shizq.bika.ui.dashboard.DashboardDestination
import com.shizq.bika.ui.dashboard.DashboardScreen
import com.shizq.bika.ui.dashboard.EditProfileDialog
import com.shizq.bika.ui.download.DownloadListScreen
import com.shizq.bika.ui.feed.FeedScreen
import com.shizq.bika.ui.feed.FeedViewModel
import com.shizq.bika.ui.history.HistoryScreen
import com.shizq.bika.ui.leaderboard.LeaderboardScreen
import com.shizq.bika.ui.notifications.NotificationsScreen
import com.shizq.bika.ui.search.SearchScreen
import com.shizq.bika.ui.signin.LoginScreen
import com.shizq.bika.ui.signup.RegistrationScreen

fun EntryProviderScope<NavKey>.authenticationSection(
    navigateToRegister: () -> Unit,
    onBackClick: () -> Unit,
    useAnimation: Boolean = true
) {
    entry<AuthenticationRoute.LoginRoute> {
        LoginScreen(
            onNavigateToSignUp = navigateToRegister,
            onNavigateToForgotPassword = {},
            onNavigateToDashboard = {}
        )
    }
    entry<AuthenticationRoute.RegisterRoute>(
        metadata = slideTransitionMetadata(useAnimation)
    ) {
        RegistrationScreen(
            onBackClick = onBackClick
        )
    }
}

private fun slideTransitionMetadata(useAnimation: Boolean = true) = metadata {
    if (useAnimation) {
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
    } else {
        put(NavDisplay.TransitionKey) {
            EnterTransition.None togetherWith ExitTransition.None
        }
        put(NavDisplay.PopTransitionKey) {
            EnterTransition.None togetherWith ExitTransition.None
        }
        put(NavDisplay.PredictivePopTransitionKey) {
            EnterTransition.None togetherWith ExitTransition.None
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.featureSection(
    navigator: Navigator,
    onLogout: () -> Unit,
    useAnimation: Boolean = true
) {
    fun slideTransitionMetadata() = slideTransitionMetadata(useAnimation)

    entry<ConnectedRoute.DashboardRoute> {
        DashboardScreen(onNavigate = { navigator.navigate(it.toNavKey()) })
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
            onBlockedTagsClick = { navigator.navigate(ConnectedRoute.BlockedTagsRoute) },
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
    ) {
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
            navigationToLogin = onLogout,
            navigationToStorageManager = { navigator.navigate(ConnectedRoute.StorageManagerRoute) },
            navigationToDnsSettings = { navigator.navigate(ConnectedRoute.DnsSettingsRoute) },
            navigationToBlockedTags = { navigator.navigate(ConnectedRoute.BlockedTagsRoute) },
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
    entry<ConnectedRoute.DnsSettingsRoute>(
        metadata = slideTransitionMetadata()
    ) {
        DnsSettingsScreen(
            onBackClick = navigator::goBack
        )
    }
    entry<ConnectedRoute.BlockedTagsRoute>(
        metadata = slideTransitionMetadata()
    ) {
        BlockedTagsScreen(
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
            },
            onTagClick = {
                navigator.navigate(TagBlockDialogNavKey(it))
            },
            navigationToEpisodeDownload = { comicId, title, coverUrl ->
                navigator.navigate(
                    EpisodeDownloadSheetNavKey(comicId, title, coverUrl)
                )
            },
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

    entry<EditProfileNavKey>(
        metadata = DialogSceneStrategy.dialog(),
    ) { key ->
        EditProfileDialog(
            initialSlogan = key.initialSlogan,
            onDismiss = navigator::goBack,
            onChangePasswordClick = { navigator.navigate(ChangePasswordNavKey) },
        )
    }

    entry<ChangePasswordNavKey>(
        metadata = DialogSceneStrategy.dialog(),
    ) {
        ChangePasswordDialog(
            onDismiss = navigator::goBack,
        )
    }
    entry<EpisodeDownloadSheetNavKey>(
        metadata = BottomSheetSceneStrategy.bottomSheet(),
    ) { key ->
        EpisodeDownloadSheet(
            onDismiss = navigator::goBack,
            viewModel = hiltViewModel<EpisodeDownloadViewModel, EpisodeDownloadViewModel.Factory>(
                key = key.toString(),
            ) { factory ->
                factory.create(key.comicId, key.comicTitle, key.coverUrl)
            },
        )
    }

    entry<TagBlockDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(),
    ) { key ->
        TagBlockDialog(
            tag = key.tag,
            onDismiss = navigator::goBack,
        )
    }
}



fun Navigator.navigateToUnitedDetail(id: String) {
    navigate(ConnectedRoute.UnitedDetailRoute(id))
}

/**
 * Dashboard 的意图 → 主图路由。
 *
 * 映射放在导航层而不是屏幕内：屏幕只声明「想去哪」，路由表仍然只有这里知道。
 * 穷尽的 `when` 是这个方案的主要收益——[DashboardDestination] 新增成员时这里
 * 编译失败，而原先的多回调形状只会静默漏接一根线。
 */
private fun DashboardDestination.toNavKey(): Connected = when (this) {
    DashboardDestination.Leaderboard -> ConnectedRoute.LeaderboardRoute
    is DashboardDestination.Feed -> ConnectedRoute.FeedRoute(action)
    DashboardDestination.History -> ConnectedRoute.HistoryRoute
    DashboardDestination.Settings -> ConnectedRoute.SettingsRoute
    is DashboardDestination.Reader -> ConnectedRoute.ReaderRoute(comicId, chapterOrder)
    DashboardDestination.Search -> ConnectedRoute.SearchRoute
    DashboardDestination.ChannelPreference -> ChannelSettingsNavKey
    is DashboardDestination.EditProfile -> EditProfileNavKey(initialSlogan)
    DashboardDestination.Comments -> ConnectedRoute.MineCommentRoute
    DashboardDestination.Downloads -> ConnectedRoute.DownloadListRoute
    DashboardDestination.Notifications -> ConnectedRoute.NotificationsRoute
    DashboardDestination.BlockedTags -> ConnectedRoute.BlockedTagsRoute
}