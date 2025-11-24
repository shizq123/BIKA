package com.shizq.bika.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.shizq.bika.utils.SPUtil
import dagger.hilt.android.AndroidEntryPoint
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
        val dashboardSections by viewModel.sectionsFlow.collectAsStateWithLifecycle()
        DashboardContent(
            dashboardState = dashboardSections,
            onRetry = viewModel::restart
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardContent(dashboardState: DashboardUiState, onRetry: () -> Unit) {
        val drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        BackHandler(enabled = drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DashboardDrawerContent(UserProfile())
                }
            },
        ) {
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
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
                            IconButton(
                                onClick = {
                                    startActivity(
                                        Intent(this@MainActivity, SearchActivity::class.java)
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    when (dashboardState) {
                        DashboardUiState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        is DashboardUiState.Error -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "加载失败: ${dashboardState.message}")
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onRetry) {
                                    Text("重试")
                                }
                            }
                        }

                        is DashboardUiState.Success -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                state = state,
                            ) {
                                items(
                                    dashboardState.dashboardEntries,
                                    key = { it.hashCode() }
                                ) { section ->
                                    when (section) {
                                        is DashboardEntry.Native -> FeatureEntry(
                                            section.imageResId,
                                            section.titleResId
                                        ) {
                                            navigateToDashboardEntry(section)
                                        }

                                        is DashboardEntry.Remote -> FeatureEntry(
                                            section.imageUrl,
                                            section.title,
                                        ) {
                                            navigateToDashboardEntry(section)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun navigateToDashboardEntry(entry: DashboardEntry) {
        when (entry) {
            is DashboardEntry.Native -> {
                when (entry.type) {
                    EntryType.RECOMMEND -> start(CollectionsActivity::class.java)

                    EntryType.LEADERBOARD -> start(LeaderboardActivity::class.java)

                    EntryType.GAME -> start(GamesActivity::class.java)

                    EntryType.APPS -> start(AppsActivity::class.java)

                    EntryType.CHAT -> start(ChatRoomListActivity::class.java)

                    EntryType.FORUM -> {
                        val intent = Intent(this, CommentsActivity::class.java).apply {
                            putExtra("id", "5822a6e3ad7ede654696e482")
                            putExtra("comics_games", "comics")
                        }
                        startActivity(intent)
                    }

                    EntryType.LATEST, EntryType.RANDOM -> {
                        val targetClass = ComicListActivity::class.java
                        val intent = Intent(this, targetClass).apply {
                            val tagValue =
                                if (entry.type == EntryType.LATEST) "latest" else "random"
                            val titleValue = getString(entry.titleResId)

                            putExtra("tag", tagValue)
                            putExtra("title", titleValue)
                            putExtra("value", titleValue)
                        }
                        startActivity(intent)
                    }
                }
            }

            is DashboardEntry.Remote -> {
                if (entry.isWeb && !entry.link.isNullOrEmpty()) {
                    val token = SPUtil.get("token", "")
                    val secret = "pb6XkQ94iBBny1WUAxY0dY5fksexw0dt"
                    val fullUrl = "${entry.link}/?token=$token&secret=$secret"

                    val intent = Intent(Intent.ACTION_VIEW, fullUrl.toUri())
                    startActivity(intent)
                } else {
                    val intent = Intent(this, ComicListActivity::class.java)
                    intent.putExtra("tag", "categories")
                    intent.putExtra("title", entry.title)
                    intent.putExtra("value", entry.title)
                    startActivity(intent)
                }
            }
        }
    }

    private fun start(clazz: Class<*>) {
        startActivity(Intent(this, clazz))
    }

    @Composable
    fun DashboardDrawerContent(
        userProfile: UserProfile,
        modifier: Modifier = Modifier,
        onPunchIn: () -> Unit = {},
        onAvatarClick: () -> Unit = {},
    ) {
        Column(modifier = modifier) {
            Column(
                modifier = modifier.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 头像容器
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clickable { onAvatarClick() }) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(userProfile.avatarUrl)
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
                            text = "Lv.${userProfile.level}",
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
                            .clickable {
                                startActivity(Intent(this@MainActivity, UserActivity::class.java))
                            }
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
                    onClick = {
                        startActivity(Intent(this@MainActivity, HistoryActivity::class.java))
                    },
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
    
//
//
//    private fun initListener() {
//        //侧滑
//        binding.drawerLayout.addDrawerListener(
//            ActionBarDrawerToggle(
//                this@MainActivity,
//                binding.drawerLayout,
//                binding.toolbar,
//                R.string.drawer_show,
//                R.string.drawer_hide
//            )
//        )
//        (binding.mainNavView.getHeaderView(0)
//            .findViewById<TextView>(R.id.main_drawer_modify)!!).setOnClickListener {
//            startActivity(Intent(this@MainActivity, UserActivity::class.java))
//        }
//        (binding.mainNavView.getHeaderView(0)
//            .findViewById<TextView>(R.id.main_drawer_punch_in)!!).setOnClickListener {
//            (binding.mainNavView.getHeaderView(0)
//                .findViewById<TextView>(R.id.main_drawer_punch_in)!!).visibility=View.GONE
//            viewModel.punch_In()
//        }
//        (binding.mainNavView.getHeaderView(0)
//            .findViewById<ImageView>(R.id.main_drawer_character)!!).setOnClickListener {
//            if (viewModel.userId != "" && viewModel.fileServer != "") {
//                //判断用户是否登录是否有头像，是就查看头像大图
//                val intent = Intent(this, ImageActivity::class.java)
//                intent.putExtra("fileserver", viewModel.fileServer)
//                intent.putExtra("imageurl", viewModel.path)
//                val options = ActivityOptions.makeSceneTransitionAnimation(
//                    this,
//                    (binding.mainNavView.getHeaderView(0)
//                        .findViewById<ImageView>(R.id.main_drawer_imageView)!!),
//                    "image"
//                )
//                startActivity(intent, options.toBundle())
//            }
//        }
//        binding.mainNavView.setNavigationItemSelectedListener {
//            binding.mainNavView.setCheckedItem(it)
//            when (it.itemId) {
//                R.id.drawer_menu_history -> {
//                    startActivity(HistoryActivity::class.java)
//                }
//                R.id.drawer_menu_bookmark -> {
//                    val intent = Intent(this@MainActivity, ComicListActivity::class.java)
//                    intent.putExtra("tag", "favourite")
//                    intent.putExtra("title", "我的收藏")
//                    intent.putExtra("value", "我的收藏")
//                    startActivity(intent)
//
//                }
//                R.id.drawer_menu_mail -> {
//                    startActivity(NotificationsActivity::class.java)
//
//                }
//                R.id.drawer_menu_chat -> {
//                    startActivity(MyCommentsActivity::class.java)
//
//                }
//                R.id.drawer_menu_settings -> {
//                    startActivity(SettingsActivity::class.java)
//                }
//
//            }
//            true
//        }
//
//    }
//

}