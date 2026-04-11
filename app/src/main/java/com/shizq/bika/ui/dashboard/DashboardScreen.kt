package com.shizq.bika.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.model.Channel
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.ui.chatroom.current.roomlist.ChatRoomListActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    navigationToLeaderboard: () -> Unit,
    navigateToFavourite: (DiscoveryAction) -> Unit,
    navigationToHistory: () -> Unit,
    navigationToSettings: () -> Unit,
    navigateToGame: () -> Unit,
    onSearchClick: () -> Unit,
    onChannelPreferenceClick: () -> Unit,
    onCommentsClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val userProfileUiState by viewModel.userProfileUiState.collectAsStateWithLifecycle()
    val channelSettingsUiState by viewModel.userChannelPreferences.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onCheckIn()
    }
    DashboardContent(
        userProfileUiState = userProfileUiState,
        onCheckInClick = viewModel::onCheckIn,
        channelSettingsUiState = channelSettingsUiState,
        navigationToLeaderboard = navigationToLeaderboard,
        navigateToFavourite = navigateToFavourite,
        navigationToHistory = navigationToHistory,
        navigationToSettings = navigationToSettings,
        navigateToGame = navigateToGame,
        onSearchClick = onSearchClick,
        onChannelPreferenceClick = onChannelPreferenceClick,
        onCommentsClick = onCommentsClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    userProfileUiState: UserProfileUiState,
    onCheckInClick: () -> Unit,
    channelSettingsUiState: List<Channel>,
    navigationToLeaderboard: () -> Unit,
    navigateToFavourite: (DiscoveryAction) -> Unit,
    navigationToHistory: () -> Unit,
    navigationToSettings: () -> Unit,
    navigateToGame: () -> Unit,
    onSearchClick: () -> Unit,
    onChannelPreferenceClick: () -> Unit,
    onCommentsClick: () -> Unit,
) {
    val drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        scope.launch {
            if (drawerState.isOpen) {
                delay(500)
                drawerState.close()
            }
        }
    }
    ModalNavigationDrawer(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(200.dp)
                    .testTag("dashboard:drawer"),
                drawerState = drawerState
            ) {
                DashboardDrawerContent(
                    userProfile = userProfileUiState,
                    onCheckInClick = {
                        scope.launch {
                            drawerState.close()
                            onCheckInClick()
                        }
                    },
                    onEditProfileClick = {

                    },
                    onHistoryClick = {
                        scope.launch {
                            drawerState.close()
                            navigationToHistory()
                        }
                    },
                    onFavouriteClick = {
                        scope.launch {
                            drawerState.close()
                            navigateToFavourite(DiscoveryAction.ToFavourite)
                        }
                    },
                    onNotificationsClick = {

                    },
                    onCommentsClick = {
                        scope.launch {
                            drawerState.close()
                            onCommentsClick()
                        }
                    },
                    onSettingsClick = {
                        scope.launch {
                            drawerState.close()
                            navigationToSettings()
                        }
                    },
                )
            }
        },
    ) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            topBar = {
                DashboardAppBar(
                    scrollBehavior = scrollBehavior,
                    onDrawerOpen = { scope.launch { drawerState.open() } },
                    onSearchClicked = onSearchClick,
                    onChannelPreferenceClicked = onChannelPreferenceClick,
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { innerPadding ->
            val state: LazyGridState = rememberLazyGridState()

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("dashboard:grid"),
            ) {
                items(
                    channelSettingsUiState,
                    key = { it.resName }
                ) { item ->
                    val context = LocalContext.current

                    if (item.isActive) {
                        ChannelGridItem(
                            iconRes = item.iconResId,
                            label = item.displayName,
                            modifier = Modifier
                                .animateItem()
                                .testTag("dashboard:channel:${item.displayName}"),
                        ) {
                            navigation(
                                context = context,
                                channel = item,
                                navigationToLeaderboard = navigationToLeaderboard,
                                navigateToSearch = navigateToFavourite,
                                navigateToGame = navigateToGame,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onDrawerOpen: () -> Unit,
    onSearchClicked: () -> Unit,
    onChannelPreferenceClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.testTag("dashboard:appbar"),
        title = { Text("哔咔") },
        navigationIcon = {
            IconButton(onClick = onDrawerOpen, modifier = Modifier.testTag("dashboard:menu")) {
                Icon(Icons.Default.Menu, contentDescription = "打开菜单")
            }
        },
        actions = {
            IconButton(
                onClick = onChannelPreferenceClicked,
                modifier = Modifier.testTag("dashboard:filter")
            ) {
                Icon(Icons.Filled.FilterList, contentDescription = "Channel Filter")
            }

            IconButton(onClick = onSearchClicked, modifier = Modifier.testTag("dashboard:search")) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

private fun navigation(
    channel: Channel,
    context: Context,
    navigationToLeaderboard: () -> Unit,
    navigateToSearch: (DiscoveryAction) -> Unit,
    navigateToGame: () -> Unit,
) {
//    if (channel.link != null) {
//        val token = SPUtil.get("token", "")
//        val secret = "pb6XkQ94iBBny1WUAxY0dY5fksexw0dt"
//        val fullUrl = "${channel.link}/?token=$token&secret=$secret"
//
//        val intent = Intent(Intent.ACTION_VIEW, fullUrl.toUri())
//        context.startActivity(intent)
//        return
//    }

    when (channel.displayName) {
        "推荐" -> navigateToSearch(DiscoveryAction.ToCollections)
        "排行榜" -> navigationToLeaderboard()
        "游戏推荐" -> navigateToGame()
//            "哔咔小程序" -> start(AppsActivity::class.java)
        "留言板" -> {
            val intent = Intent(context, ChatRoomListActivity::class.java)
            context.startActivity(intent)
        }

        "最近更新" -> navigateToSearch(DiscoveryAction.ToRecent)

        "随机本子" -> navigateToSearch(DiscoveryAction.ToRandom)

        else -> navigateToSearch(
            DiscoveryAction.Channel(channel.displayName)
        )
    }
}