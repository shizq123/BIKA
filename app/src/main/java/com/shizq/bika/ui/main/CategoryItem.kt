package com.shizq.bika.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shizq.bika.R
import com.shizq.bika.ui.apps.AppsActivity
import com.shizq.bika.ui.chatroom.current.roomlist.ChatRoomListActivity
import com.shizq.bika.ui.collections.CollectionsActivity
import com.shizq.bika.ui.comiclist.ComicListActivity
import com.shizq.bika.ui.comment.CommentsActivity
import com.shizq.bika.ui.games.GamesActivity
import com.shizq.bika.ui.history.HistoryActivity
import com.shizq.bika.ui.leaderboard.LeaderboardActivity
import com.shizq.bika.ui.search.SearchActivity
import com.shizq.bika.ui.user.UserActivity
import kotlinx.coroutines.launch

data class CategoryItem(
    val title: String,
    val imageRes: Int = 0, // 用于本地图标判断
    val imageUrl: String? = null,
    val isWeb: Boolean = false,
    val link: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(dashboardViewModel: DashboardViewModel = viewModel()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
//    val uiState by mainViewModel.uiState.collectAsState()
//    val userProfile by mainViewModel.userProfile.collectAsState()

    // 侧滑菜单容器
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("哔咔") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            context.startActivity(Intent(context, SearchActivity::class.java))
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
//                when (val state = uiState) {
//                    is UiState.Loading -> {
//                        Column(
//                            modifier = Modifier.align(Alignment.Center),
//                            horizontalAlignment = Alignment.CenterHorizontally
//                        ) {
//                            CircularProgressIndicator()
//                            Text("加载中...", modifier = Modifier.padding(top = 8.dp))
//                        }
//                    }
//                    is UiState.Error -> {
//                        Column(
//                            modifier = Modifier
//                                .align(Alignment.Center)
//                                .clickable {/* mainViewModel.retry() */},
//                            horizontalAlignment = Alignment.CenterHorizontally
//                        ) {
//                            Icon(
//                                Icons.Default.Refresh,
//                                contentDescription = "Retry",
//                                modifier = Modifier.size(48.dp),
//                                tint = MaterialTheme.colorScheme.error
//                            )
//                            Text(
//                                text = state.message + "\n点击重试",
//                                color = MaterialTheme.colorScheme.error,
//                                modifier = Modifier.padding(top = 8.dp)
//                            )
//                        }
//                    }
//                    is UiState.Success -> {
//                        val configuration = LocalConfiguration.current
//                        val screenWidth = configuration.screenWidthDp
//                        val screenHeight = configuration.screenHeightDp
//                        // 简单适配逻辑：横屏或宽屏显示6列，否则3列
//                        val columns = if (screenWidth > screenHeight) 6 else 3
//
//                        LazyVerticalGrid(
//                            columns = GridCells.Fixed(columns),
//                            contentPadding = PaddingValues(8.dp),
//                            verticalArrangement = Arrangement.spacedBy(8.dp),
//                            horizontalArrangement = Arrangement.spacedBy(8.dp)
//                        ) {
//                            items(state.categories) { item ->
//                                CategoryItemView(item) {
//                                    handleCategoryClick(context, item)
//                                }
//                            }
//                        }
//                    }
//                }
            }
        }
    }
}

@Composable
fun MainDrawerContent(
    userProfile: UserProfile,
    onPunchIn: () -> Unit,
    onNavigate: (Intent) -> Unit,
    onAvatarClick: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.bika),
                contentDescription = null,
                contentScale = ContentScale.Crop,
//                modifier = Modifier.fillMaxSize().alpha(0.3f)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomStart)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 头像容器
                    Box(modifier = Modifier
                        .size(64.dp)
                        .clickable { onAvatarClick() }) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(userProfile.avatarUrl)
                                .crossfade(true)
                                .placeholder(R.drawable.placeholder_avatar_2) // 假设资源存在
                                .build(),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        // 头像框
                        if (userProfile.frameUrl.isNotEmpty()) {
                            AsyncImage(
                                model = userProfile.frameUrl,
                                contentDescription = "Frame",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lv.${userProfile.level} (${userProfile.exp}/${
                                expCalc(
                                    userProfile.level
                                )
                            })",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = userProfile.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val genderText = when (userProfile.gender) {
                        "m" -> "(绅士)"
                        "f" -> "(淑女)"
                        else -> "(机器人)"
                    }
                    Text(text = genderText, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = userProfile.slogan.ifEmpty { "这个人很懒，什么都没写" },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Text(
                        text = "修改资料",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onNavigate(Intent(context, UserActivity::class.java)) }
                            .padding(end = 16.dp)
                    )

                    if (!userProfile.isPunched) {
                        Text(
                            text = "打卡",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onPunchIn() }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // --- Menu Items ---
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            NavigationDrawerItem(
                label = { Text("首页") },
                selected = true, // 这里简单处理，实际应根据路由判断
                onClick = { /* Stay here */ },
                icon = { Icon(painterResource(R.drawable.ic_home), null) }, // 需替换对应图标
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text("历史记录") },
                selected = false,
                onClick = { onNavigate(Intent(context, HistoryActivity::class.java)) },
                icon = { Icon(painterResource(R.drawable.ic_history), null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
//            NavigationDrawerItem(
//                label = { Text("我的收藏") },
//                selected = false,
//                onClick = {
//                    val intent = Intent(context, ComicListActivity::class.java).apply {
//                        putExtra("tag", "favourite")
//                        putExtra("title", "我的收藏")
//                    }
//                    onNavigate(intent)
//                },
//                icon = { Icon(painterResource(R.drawable.ic_bookmark), null) },
//                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
//            )
//            NavigationDrawerItem(
//                label = { Text("我的消息") },
//                selected = false,
//                onClick = { onNavigate(Intent(context, NotificationsActivity::class.java)) },
//                icon = { Icon(painterResource(R.drawable.ic_email), null) },
//                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
//            )
//             NavigationDrawerItem(
//                label = { Text("我的评论") },
//                selected = false,
//                onClick = { onNavigate(Intent(context, MyCommentsActivity::class.java)) },
//                icon = { Icon(painterResource(R.drawable.ic_comment), null) },
//                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
//            )
//            NavigationDrawerItem(
//                label = { Text("设置") },
//                selected = false,
//                onClick = { onNavigate(Intent(context, SettingsActivity::class.java)) },
//                icon = { Icon(painterResource(R.drawable.ic_settings), null) },
//                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
//            )
        }
    }
}

@Composable
fun CategoryItemView(item: CategoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f) // 调整卡片比例
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 如果有网络图片则加载网络图片，否则加载本地资源（根据原逻辑，很多是本地 drawable）
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.size(64.dp)
                )
            } else if (item.imageRes != 0) {
                Image(
                    painter = painterResource(id = item.imageRes),
                    contentDescription = item.title,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 处理主页 Grid 点击事件
fun handleCategoryClick(context: Context, item: CategoryItem) {
    // 为了兼容 Context.startActivity，这里需要进行强转或者确保 context 是 Activity
    val activity = context as? Activity ?: return

    // 根据原逻辑中的 ResId 判断 (在 Compose 中最好用 Enum 或 Type 判断，这里沿用旧逻辑的变体)
    // 假设 CategoryItem 中包含了一个 type 字段或者根据 imageRes 判断
    // 这里简化演示
    val intent = when (item.imageRes) {
        R.drawable.bika -> Intent(context, CollectionsActivity::class.java)
        R.drawable.cat_leaderboard -> Intent(context, LeaderboardActivity::class.java)
        R.drawable.cat_game -> Intent(context, GamesActivity::class.java)
        R.drawable.cat_love_pica -> Intent(context, AppsActivity::class.java)
        R.drawable.ic_chat -> Intent(context, ChatRoomListActivity::class.java)
        R.drawable.cat_forum -> Intent(context, CommentsActivity::class.java).apply {
            putExtra("id", "5822a6e3ad7ede654696e482")
            putExtra("comics_games", "comics")
        }

        R.drawable.cat_latest -> Intent(context, ComicListActivity::class.java).apply {
            putExtra("tag", "latest")
            putExtra("title", item.title)
        }

        R.drawable.cat_random -> Intent(context, ComicListActivity::class.java).apply {
            putExtra("tag", "random")
            putExtra("title", item.title)
        }

        else -> {
            if (item.isWeb) {
                Intent(
                    "android.intent.action.VIEW",
                    Uri.parse(item.link)
                ) // token logic omitted for brevity
            } else {
                Intent(context, ComicListActivity::class.java).apply {
                    putExtra("tag", "categories")
                    putExtra("title", item.title)
                }
            }
        }
    }
    context.startActivity(intent)
}

// 辅助函数：简单的 Modifier 透明度扩展
fun Modifier.alpha(alpha: Float) = this.then(Modifier
    .drawWithContent {
        drawContent()
        drawRect(
            Color.Black.copy(alpha = 1f - alpha),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
        )
        // 注意：这里只是个伪代码示例，实际建议用 graphicsLayer { this.alpha = alpha }
    }
    .then(graphicsLayer { this.alpha = alpha }))

// 经验值计算公式
fun expCalc(level: Int): Int {
    return 100 * level * level + (100 * level)
}