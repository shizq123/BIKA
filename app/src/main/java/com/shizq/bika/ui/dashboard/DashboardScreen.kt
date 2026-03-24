package com.shizq.bika.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import com.shizq.bika.R
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.ui.CircularProgressIndicator
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
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
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
                    .padding(innerPadding),
            ) {
                items(
                    channelSettingsUiState,
                    key = { it.resName }
                ) { item ->
                    val context = LocalContext.current
                    val resources = LocalResources.current

                    if (item.isActive) {
                        val resId = resources.getIdentifier(
                            item.resName,
                            "drawable",
                            context.packageName
                        )

                        ChannelGridItem(
                            iconRes = resId,
                            label = item.displayName,
                            modifier = Modifier.animateItem(),
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
        modifier = modifier,
        title = { Text("哔咔") },
        navigationIcon = {
            IconButton(onClick = onDrawerOpen) {
                Icon(Icons.Default.Menu, contentDescription = "打开菜单")
            }
        },
        actions = {
            IconButton(onClick = onChannelPreferenceClicked) {
                Icon(Icons.Filled.FilterList, contentDescription = "Channel Filter")
            }

            IconButton(
                onClick = onSearchClicked
            ) {
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

@Composable
fun DashboardDrawerContent(
    userProfile: UserProfileUiState,
    modifier: Modifier = Modifier,
    onCheckInClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onCommentsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    when (userProfile) {
        is UserProfileUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载用户信息失败")
            }
        }

        UserProfileUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UserProfileUiState.Success -> {
            Column(modifier = modifier.fillMaxSize()) {
                UserProfileCard(
                    state = userProfile,
                    onCheckInClick = onCheckInClick,
                    onEditProfile = onEditProfileClick
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    DrawerMenuItem(
                        label = "历史记录",
                        iconRes = R.drawable.ic_history,
                        onClick = onHistoryClick
                    )
                    DrawerMenuItem(
                        label = "我的收藏",
                        iconVector = Icons.Filled.Favorite,
                        onClick = onFavouriteClick
                    )
                    DrawerMenuItem(
                        label = "我的消息",
                        iconVector = Icons.Filled.Email,
                        onClick = onNotificationsClick
                    )
                    DrawerMenuItem(
                        label = "我的评论",
                        iconVector = Icons.AutoMirrored.Filled.Comment,
                        onClick = onCommentsClick
                    )
                    DrawerMenuItem(
                        label = "设置",
                        iconVector = Icons.Filled.Settings,
                        onClick = onSettingsClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    iconRes: Int? = null,
    iconVector: ImageVector? = null
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        icon = {
            if (iconRes != null) {
                Icon(painterResource(iconRes), contentDescription = label)
            } else if (iconVector != null) {
                Icon(iconVector, contentDescription = label)
            }
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
fun UserProfileCard(
    state: UserProfileUiState.Success,
    modifier: Modifier = Modifier,
    onCheckInClick: () -> Unit,
    onEditProfile: () -> Unit,
) {
    val user = state.user
    Column(
        modifier = modifier
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.avatarUrl)
                        .placeholder(R.drawable.placeholder_avatar_2)
                        .error(R.drawable.placeholder_avatar_2)
                        .build(),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.levelDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = user.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onEditProfile
                ) {
                    Text(text = "修改资料")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onCheckInClick,
                    enabled = !user.hasCheckedIn,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(text = if (user.hasCheckedIn) "已打卡" else "打卡")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val genderText = remember(user.gender) {
                when (user.gender) {
                    "m" -> "绅士"
                    "f" -> "淑女"
                    else -> "机器人"
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = genderText,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = user.slogan.ifEmpty { "这个人很懒，什么都没写" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}