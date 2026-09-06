package com.shizq.bika.ui.dashboard

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.shizq.bika.R
import com.shizq.bika.core.data.model.DetailedReadingHistory
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.feature.settings.impl.update.ui.UpdateHost
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.ui.feed.FavoriteTagsDrawer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    navigationToLeaderboard: () -> Unit,
    navigateToFavourite: (DiscoveryAction) -> Unit,
    navigationToHistory: () -> Unit,
    navigationToSettings: () -> Unit,
    onSearchClick: () -> Unit,
    onChannelPreferenceClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    navigationToReader: (String, Int) -> Unit,
    onBlockedTagsClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val callbacks = remember(
        navigationToLeaderboard,
        navigateToFavourite,
        navigationToHistory,
        navigationToSettings,
        navigationToReader,
        onSearchClick,
        onChannelPreferenceClick,
        onCommentsClick,
        onDownloadsClick,
        onNotificationsClick,
        onBlockedTagsClick,
    ) {
        DashboardCallbacks(
            navigateToLeaderboard = navigationToLeaderboard,
            navigateToFavourite = navigateToFavourite,
            navigateToHistory = navigationToHistory,
            navigateToSettings = navigationToSettings,
            navigateToReader = navigationToReader,
            onSearchClick = onSearchClick,
            onChannelPreferenceClick = onChannelPreferenceClick,
            onCommentsClick = onCommentsClick,
            onDownloadsClick = onDownloadsClick,
            onNotificationsClick = onNotificationsClick,
            onBlockedTagsClick = onBlockedTagsClick,
        )
    }

    // 自动打卡：profile 加载成功后 dispatch 一次，实际检查逻辑在 StateMachine 内部完成。
    // key 用 Boolean 而非整个 userProfile：后者每次资料刷新（打卡改了 exp/level、
    // 改签名、下拉刷新）都会换实例，导致 effect 重启并重复 dispatch。
    val isProfileLoaded = state.userProfile is UserProfileUiState.Success
    LaunchedEffect(isProfileLoaded) {
        if (isProfileLoaded) {
            viewModel.dispatch(DashboardAction.AutoCheckIn)
        }
    }

    // 打卡结果对话框（状态驱动）
    val checkInResult = state.checkInResult
    if (checkInResult != null) {
        CheckInResultDialog(
            result = checkInResult,
            onDismiss = { viewModel.dispatch(DashboardAction.DismissCheckInResult) },
        )
    }

    UpdateHost()

    DashboardContent(
        state = state,
        onAction = viewModel::dispatch,
        callbacks = callbacks,
    )
}

/** 修改资料 / 修改密码两个对话框互斥，用同一个状态仲裁哪个在显示。 */
private sealed interface DashboardDialog {
    data object EditProfile : DashboardDialog
    data object ChangePassword : DashboardDialog
}

@Composable
fun DashboardContent(
    state: DashboardState,
    onAction: (DashboardAction) -> Unit,
    callbacks: DashboardCallbacks,
) {
    val drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val userProfileUiState = state.userProfile
    val lastReadHistory = state.lastReadHistory
    val activeChannels = state.activeChannels
    val favoriteTags = state.favoriteTags
    val sloganResult = state.sloganResult
    val passwordResult = state.passwordResult
    val isSubmitting = state.isSubmitting

    val onCheckInClick = { onAction(DashboardAction.CheckIn) }
    val onUpdateSlogan = { slogan: String -> onAction(DashboardAction.UpdateSlogan(slogan)) }
    val onDismissSloganResult = { onAction(DashboardAction.DismissSloganResult) }
    val onChangePassword = { old: String, new: String ->
        onAction(DashboardAction.ChangePassword(old, new))
    }
    val onDismissPasswordResult = { onAction(DashboardAction.DismissPasswordResult) }
    val onAddFavorite = { tag: FavoriteTag -> onAction(DashboardAction.AddFavoriteTag(tag)) }
    val onRemoveFavorite = { tag: FavoriteTag -> onAction(DashboardAction.RemoveFavoriteTag(tag)) }
    val onUpdateFavoriteName = { tag: FavoriteTag, name: String ->
        onAction(DashboardAction.UpdateFavoriteTagName(tag, name))
    }
    val onMoveFavorite = { from: Int, to: Int ->
        onAction(DashboardAction.MoveFavoriteTag(from, to))
    }
    val onAddCustomFavorite = { name: String ->
        onAction(DashboardAction.AddCustomFavoriteTag(name))
    }

    // ── 对话框状态（互斥）────────────────────────────────────────────────
    // 修改资料 / 修改密码同一时刻只会显示一个，且两者可以互相跳转，
    // 用一个 sealed 状态仲裁比两个独立 Boolean 更能表达这种互斥关系。
    var activeDialog by remember { mutableStateOf<DashboardDialog?>(null) }
    var editProfileInitialSlogan by remember { mutableStateOf("") }

    if (activeDialog == DashboardDialog.EditProfile) {
        EditProfileDialog(
            initialSlogan = editProfileInitialSlogan,
            sloganResult = sloganResult,
            isSubmitting = isSubmitting,
            onSave = onUpdateSlogan,
            onDismissResult = onDismissSloganResult,
            onDismiss = { activeDialog = null },
            onChangePasswordClick = { activeDialog = DashboardDialog.ChangePassword },
        )
    }

    if (activeDialog == DashboardDialog.ChangePassword) {
        ChangePasswordDialog(
            passwordResult = passwordResult,
            isSubmitting = isSubmitting,
            onSave = onChangePassword,
            onDismissResult = onDismissPasswordResult,
            onDismiss = { activeDialog = null },
        )
    }

    var showBookmarkDrawer by remember { mutableStateOf(false) }

    BackHandler(enabled = showBookmarkDrawer) {
        showBookmarkDrawer = false
    }

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
    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            modifier = Modifier.semantics { testTagsAsResourceId = true },
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.testTag("dashboard:drawer"),
                    drawerState = drawerState
                ) {
                    DashboardDrawerContent(
                        userProfile = userProfileUiState,
                        lastReadHistory = lastReadHistory,
                        navigationToReader = { comicId, order ->
                            scope.launch {
                                drawerState.close()
                                callbacks.navigateToReader(comicId, order)
                            }
                        },
                        onCheckInClick = {
                            scope.launch {
                                drawerState.close()
                                onCheckInClick()
                            }
                        },
                        onEditProfileClick = {
                            scope.launch {
                                drawerState.close()
                                // 同步初始化签名输入框初值，避免子组件用旧值 remember
                                if (userProfileUiState is UserProfileUiState.Success) {
                                    editProfileInitialSlogan = userProfileUiState.user.slogan
                                }
                                activeDialog = DashboardDialog.EditProfile
                            }
                        },
                        onHistoryClick = {
                            scope.launch {
                                drawerState.close()
                                callbacks.navigateToHistory()
                            }
                        },
                        onFavouriteClick = {
                            scope.launch {
                                drawerState.close()
                                callbacks.navigateToFavourite(DiscoveryAction.ToFavourite)
                            }
                        },
                        onNotificationsClick = {
                            scope.launch {
                                drawerState.close()
                                callbacks.onNotificationsClick()
                            }
                        },
                        onCommentsClick = {
                            scope.launch {
                                drawerState.close()
                                callbacks.onCommentsClick()
                            }
                        },
                        onDownloadsClick = {
                            scope.launch {
                                drawerState.close()
                                callbacks.onDownloadsClick()
                            }
                        },
                        onSettingsClick = {
                            scope.launch {
                                drawerState.close()
                                callbacks.navigateToSettings()
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
                        onSearchClicked = callbacks.onSearchClick,
                        onChannelPreferenceClicked = callbacks.onChannelPreferenceClick,
                        onBookmarkClicked = { showBookmarkDrawer = true },
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) { innerPadding ->
                val gridState: LazyGridState = rememberLazyGridState()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("dashboard:grid"),
                ) {
                    lastReadHistory?.let { history ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            QuickResumeCard(
                                history = history,
                                onClick = callbacks.navigateToReader,
                                modifier = Modifier
                                    .animateItem()
                            )
                        }
                    }

                    items(
                        activeChannels,
                        key = { it.iconKey }
                    ) { item ->
                        ChannelGridItem(
                            iconRes = item.iconResId,
                            label = item.label,
                            modifier = Modifier
                                .animateItem()
                                .testTag("dashboard:channel:${item.label}"),
                        ) {
                            when (val destination = item.toDestination()) {
                                is ChannelDestination.Feed ->
                                    callbacks.navigateToFavourite(destination.action)

                                ChannelDestination.Leaderboard ->
                                    callbacks.navigateToLeaderboard()

                                is ChannelDestination.Unavailable ->
                                    Toast.makeText(
                                        context,
                                        destination.reason,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                    }
                }
            }
        }

        if (showBookmarkDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showBookmarkDrawer = false
                    }
            )
        }

        AnimatedVisibility(
            visible = showBookmarkDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .align(Alignment.CenterEnd)
        ) {
            FavoriteTagsDrawer(
                favoriteTags = favoriteTags,
                currentAction = null,
                onNavigateToFeed = { action ->
                    showBookmarkDrawer = false
                    callbacks.navigateToFavourite(action)
                },
                onAddFavorite = onAddFavorite,
                onRemoveFavorite = onRemoveFavorite,
                onUpdateName = onUpdateFavoriteName,
                onMove = onMoveFavorite,
                onAddCustom = onAddCustomFavorite,
                onBlockedTagsClick = callbacks.onBlockedTagsClick,
                onClose = { showBookmarkDrawer = false }
            )
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
    onBookmarkClicked: () -> Unit,
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
                onClick = onBookmarkClicked,
                modifier = Modifier.testTag("dashboard:bookmark")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bookmarks,
                    contentDescription = "标签收藏夹"
                )
            }

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


@Composable
fun DashboardDrawerContent(
    userProfile: UserProfileUiState,
    lastReadHistory: DetailedReadingHistory?,
    navigationToReader: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    onCheckInClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onCommentsClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 用户信息卡片：Loading / Error / Success 三态统一由 UserProfileStateCard 处理
        UserProfileStateCard(
            state = userProfile,
            onCheckInClick = onCheckInClick,
            onEditProfileClick = onEditProfileClick,
        )
        HorizontalDivider()
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            lastReadHistory?.let { history ->
                QuickResumeCard(
                    history = history,
                    onClick = navigationToReader,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))
            }
            DrawerMenuItem(
                label = "历史记录",
                iconRes = R.drawable.ic_history,
                onClick = onHistoryClick,
                modifier = Modifier.testTag(DashboardDrawerTags.History)
            )
            DrawerMenuItem(
                label = "我的收藏",
                iconVector = Icons.Filled.Favorite,
                onClick = onFavouriteClick,
                modifier = Modifier.testTag(DashboardDrawerTags.Favourite)
            )
            DrawerMenuItem(
                label = "我的消息",
                iconVector = Icons.Filled.Email,
                onClick = onNotificationsClick,
                modifier = Modifier.testTag(DashboardDrawerTags.Notifications)
            )
            DrawerMenuItem(
                label = "我的评论",
                iconVector = Icons.AutoMirrored.Filled.Comment,
                onClick = onCommentsClick,
                modifier = Modifier.testTag(DashboardDrawerTags.Comments)
            )
            DrawerMenuItem(
                label = "我的下载",
                iconVector = Icons.Filled.Download,
                onClick = onDownloadsClick,
                modifier = Modifier.testTag(DashboardDrawerTags.Downloads)
            )
            DrawerMenuItem(
                label = "设置",
                iconVector = Icons.Filled.Settings,
                onClick = onSettingsClick,
                modifier = Modifier.testTag(DashboardDrawerTags.Settings)
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    iconRes: Int? = null,
    iconVector: ImageVector? = null,
    onClick: () -> Unit,
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
        modifier = modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
fun QuickResumeCard(
    history: DetailedReadingHistory,
    onClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 「最近读到哪一章」的判定属于数据模型，复用 DetailedReadingHistory 上的派生属性，
    // 不在 UI 里重算一遍 maxByOrNull
    val lastProgress = history.lastReadChapterProgress
    val chapterTitle = lastProgress?.let { "第 ${it.chapterNumber} 话" } ?: "第一话"
    val progressText = lastProgress?.let { "已读至第 ${it.currentPage} 页 / 共 ${it.pageCount} 页" }
        ?: "未开始阅读"
    val lastReadChapterOrder = lastProgress?.chapterNumber ?: 1

    ElevatedCard(
        onClick = { onClick(history.history.id, lastReadChapterOrder) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(history.history.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "继续阅读",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = history.history.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$chapterTitle · $progressText",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "继续阅读",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}