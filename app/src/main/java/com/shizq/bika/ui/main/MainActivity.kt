package com.shizq.bika.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import com.shizq.bika.ui.apps.AppsActivity
import com.shizq.bika.ui.chatroom.current.roomlist.ChatRoomListActivity
import com.shizq.bika.ui.collections.CollectionsActivity
import com.shizq.bika.ui.comiclist.ComicListActivity
import com.shizq.bika.ui.games.GamesActivity
import com.shizq.bika.ui.history.HistoryActivity
import com.shizq.bika.ui.leaderboard.LeaderboardActivity
import com.shizq.bika.ui.mycomments.MyCommentsActivity
import com.shizq.bika.ui.notifications.NotificationsActivity
import com.shizq.bika.ui.search.SearchActivity
import com.shizq.bika.ui.settings.SettingsActivity
import com.shizq.bika.ui.user.UserActivity
import com.shizq.bika.utils.SPUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            DashboardScreen()
        }
    }

    @Composable
    fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
        val userProfileUiState by viewModel.userProfileUiState.collectAsStateWithLifecycle()
        val channelSettingsUiState by viewModel.userChannelPreferences.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            viewModel.onLogin()
            viewModel.onCheckIn()
        }
        DashboardContent(
//            dashboardState = dashboardUiState,
            userProfileUiState = userProfileUiState,
            onCheckInClick = viewModel::onCheckIn,
            channelSettingsUiState = channelSettingsUiState,
            onRetry = viewModel::restart,
            onChannelToggled = viewModel::onChannelToggled,
            onOrderChange = viewModel::onChannelsReordered
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardContent(
//        dashboardState: DashboardUiState,
        userProfileUiState: UserProfileUiState,
        onCheckInClick: () -> Unit,
        onRetry: () -> Unit,
        channelSettingsUiState: List<Channel>,
        onChannelToggled: (Channel, Boolean) -> Unit,
        onOrderChange: (List<Channel>) -> Unit,
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
                ModalDrawerSheet {
                    DashboardDrawerContent(
                        userProfile = userProfileUiState,
                        onCheckInClick = onCheckInClick
                    )
                }
            },
        ) {
            val activeChannels = remember(channelSettingsUiState) {
                channelSettingsUiState.filter { it.isActive }
            }
            DashboardContent(
                activeChannels = activeChannels,
                allChannels = channelSettingsUiState,
                onChannelToggled = onChannelToggled,
                onDrawerOpen = { scope.launch { drawerState.open() } },
                onOrderChange = onOrderChange,
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardContent(
        activeChannels: List<Channel>,
        allChannels: List<Channel>,
        onChannelToggled: (Channel, Boolean) -> Unit,
        onOrderChange: (List<Channel>) -> Unit,
        onDrawerOpen: () -> Unit,
    ) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("哔咔") },
                    navigationIcon = {
                        IconButton(onClick = onDrawerOpen) {
                            Icon(Icons.Default.Menu, contentDescription = "打开菜单")
                        }
                    },
                    actions = {
                        var showChannelSettings by remember { mutableStateOf(false) }

                        IconButton(onClick = { showChannelSettings = true }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Channel Filter")
                        }
                        if (showChannelSettings) {
                            ChannelSettingsDialog(
                                channels = allChannels,
                                onDismiss = { showChannelSettings = false },
                                onChannelToggle = onChannelToggled,
                                onOrderChange = onOrderChange
                            )
                        }

                        IconButton(
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        SearchActivity::class.java
                                    )
                                )
                            }
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { innerPadding ->
            val state: LazyGridState = rememberLazyGridState()

            val context = LocalContext.current
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                items(
                    activeChannels,
                    key = { it.hashCode() }
                ) { item ->
                    if (item.isActive) {
                        val resId = context.resources.getIdentifier(
                            item.resName,
                            "drawable",
                            context.packageName
                        )

                        ChannelGridItem(
                            iconRes = resId,
                            label = item.displayName,
                            modifier = Modifier.animateItem(),
                        ) {
                            navigation(item)
                        }
                    }
                }
            }
        }
    }

    fun navigation(channel: Channel) {
        if (channel.link == null) {
            when (channel.displayName) {
                "推荐" -> start(CollectionsActivity::class.java)
                "排行榜" -> start(LeaderboardActivity::class.java)
                "游戏推荐" -> start(GamesActivity::class.java)
                "哔咔小程序" -> start(AppsActivity::class.java)
                "留言板" -> start(ChatRoomListActivity::class.java)
                "最近更新" -> {
                    val targetClass = ComicListActivity::class.java
                    val intent = Intent(this, targetClass).apply {

                        putExtra("tag", "latest")
                        putExtra("title", channel.displayName)
                        putExtra("value", channel.displayName)
                    }
                    startActivity(intent)
                }

                "随机本子" -> {
                    val targetClass = ComicListActivity::class.java
                    val intent = Intent(this, targetClass).apply {

                        putExtra("tag", "random")
                        putExtra("title", channel.displayName)
                        putExtra("value", channel.displayName)
                    }
                    startActivity(intent)
                }

                else -> {
                    val intent = Intent(this, ComicListActivity::class.java)
                    intent.putExtra("tag", "categories")
                    intent.putExtra("title", channel.displayName)
                    intent.putExtra("value", channel.displayName)
                    startActivity(intent)
                }
            }
        } else {
            val token = SPUtil.get("token", "")
            val secret = "pb6XkQ94iBBny1WUAxY0dY5fksexw0dt"
            val fullUrl = "${channel.link}/?token=$token&secret=$secret"

            val intent = Intent(Intent.ACTION_VIEW, fullUrl.toUri())
            startActivity(intent)
        }
    }
//    fun navigateToDashboardEntry(entry: DashboardEntry) {
//        when (entry) {
//            is DashboardEntry.Native -> {
//                when (entry.type) {
//                    EntryType.RECOMMEND -> start(CollectionsActivity::class.java)
//
//                    EntryType.LEADERBOARD -> start(LeaderboardActivity::class.java)
//
//                    EntryType.GAME -> start(GamesActivity::class.java)
//
//                    EntryType.APPS -> start(AppsActivity::class.java)
//
//                    EntryType.CHAT -> start(ChatRoomListActivity::class.java)
//
//                    EntryType.FORUM -> {
//                        val intent = Intent(this, CommentsActivity::class.java).apply {
//                            putExtra("id", "5822a6e3ad7ede654696e482")
//                            putExtra("comics_games", "comics")
//                        }
//                        startActivity(intent)
//                    }
//
//                    EntryType.LATEST, EntryType.RANDOM -> {
//                        val targetClass = ComicListActivity::class.java
//                        val intent = Intent(this, targetClass).apply {
//                            val tagValue =
//                                if (entry.type == EntryType.LATEST) "latest" else "random"
//                            val titleValue = getString(entry.titleResId)
//
//                            putExtra("tag", tagValue)
//                            putExtra("title", titleValue)
//                            putExtra("value", titleValue)
//                        }
//                        startActivity(intent)
//                    }
//                }
//            }
//
//            is DashboardEntry.Remote -> {
//                if (entry.isWeb && !entry.link.isNullOrEmpty()) {

//                } else {

//            }
//        }
//    }

    private fun start(clazz: Class<*>) {
        startActivity(Intent(this, clazz))
    }

    @Composable
    fun DashboardDrawerContent(
        userProfile: UserProfileUiState,
        modifier: Modifier = Modifier,
        onCheckInClick: () -> Unit = {},
    ) {
        when (userProfile) {
            is UserProfileUiState.Error -> {

            }

            UserProfileUiState.Loading -> {}
            is UserProfileUiState.Success -> {
                Column(modifier = modifier) {
                    UserProfileCard(
                        state = userProfile,
                        onCheckInClick = onCheckInClick,
                        onEditProfile = {
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    UserActivity::class.java
                                )
                            )
                        }
                    )
                    HorizontalDivider()

                    // --- Menu Items ---
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        NavigationDrawerItem(
                            label = { Text("首页") },
                            selected = true, // 这里简单处理，实际应根据路由判断
                            onClick = { /* Stay here */ },
                            icon = { Icon(painterResource(R.drawable.ic_home), null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        NavigationDrawerItem(
                            label = { Text("历史记录") },
                            selected = false,
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        HistoryActivity::class.java
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    painterResource(R.drawable.ic_history),
                                    null
                                )
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        NavigationDrawerItem(
                            label = { Text("我的收藏") },
                            selected = false,
                            onClick = {
                                val intent =
                                    Intent(
                                        this@MainActivity,
                                        ComicListActivity::class.java
                                    ).apply {
                                        putExtra("tag", "favourite")
                                        putExtra("title", "我的收藏")
                                    }
                                startActivity(intent)
                            },
                            icon = { Icon(Icons.Filled.Favorite, "Favorite") },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        NavigationDrawerItem(
                            label = { Text("我的消息") },
                            selected = false,
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        NotificationsActivity::class.java
                                    )
                                )
                            },
                            icon = { Icon(Icons.Filled.Email, "Email") },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        NavigationDrawerItem(
                            label = { Text("我的评论") },
                            selected = false,
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        MyCommentsActivity::class.java
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Comment,
                                    "Comment"
                                )
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        NavigationDrawerItem(
                            label = { Text("设置") },
                            selected = false,
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        SettingsActivity::class.java
                                    )
                                )
                            },
                            icon = { Icon(Icons.Filled.Settings, "Settings") },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
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
                .fillMaxWidth()
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
    }
}