package com.shizq.bika.ui.dashboard

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.shizq.bika.R
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.ui.CircularProgressIndicator
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.ui.dashboard.update.UpdateDialog
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
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val channelSettingsUiState by viewModel.userChannelPreferences.collectAsStateWithLifecycle()
    val lastReadHistory by viewModel.lastReadHistory.collectAsStateWithLifecycle()
    val favoriteTags by viewModel.favoriteTags.collectAsStateWithLifecycle()

    // 自动打卡：profile 首次加载成功且未打卡时 dispatch 一次，
    // 实际检查逻辑在 StateMachine 内部完成，不会重复触发
    val userProfileUiState = state.userProfile
    LaunchedEffect(userProfileUiState) {
        if (userProfileUiState is UserProfileUiState.Success) {
            viewModel.dispatch(DashboardAction.AutoCheckIn)
        }
    }

    // 打卡结果对话框（状态驱动）
    val checkInResult = state.checkInResult
    if (checkInResult != null) {
        val message = when (checkInResult) {
            is CheckInResult.Success -> checkInResult.message
            is CheckInResult.Error -> checkInResult.error
        }
        AlertDialog(
            onDismissRequest = { viewModel.dispatch(DashboardAction.DismissCheckInResult) },
            confirmButton = {
                TextButton(onClick = { viewModel.dispatch(DashboardAction.DismissCheckInResult) }) {
                    Text("确定")
                }
            },
            title = { Text("打哔咔提示") },
            text = { Text(message) },
        )
    }

    UpdateDialog()

    DashboardContent(
        userProfileUiState = userProfileUiState,
        lastReadHistory = lastReadHistory,
        onCheckInClick = { viewModel.dispatch(DashboardAction.CheckIn) },
        onUpdateSlogan = { slogan -> viewModel.dispatch(DashboardAction.UpdateSlogan(slogan)) },
        sloganResult = state.sloganResult,
        onDismissSloganResult = { viewModel.dispatch(DashboardAction.DismissSloganResult) },
        onChangePassword = { old, new ->
            viewModel.dispatch(
                DashboardAction.ChangePassword(
                    old,
                    new
                )
            )
        },
        passwordResult = state.passwordResult,
        onDismissPasswordResult = { viewModel.dispatch(DashboardAction.DismissPasswordResult) },
        channelSettingsUiState = channelSettingsUiState,
        navigationToLeaderboard = navigationToLeaderboard,
        navigateToFavourite = navigateToFavourite,
        navigationToHistory = navigationToHistory,
        navigationToSettings = navigationToSettings,
        onSearchClick = onSearchClick,
        onChannelPreferenceClick = onChannelPreferenceClick,
        onCommentsClick = onCommentsClick,
        onDownloadsClick = onDownloadsClick,
        onNotificationsClick = onNotificationsClick,
        navigationToReader = navigationToReader,
        favoriteTags = favoriteTags,
        onAddFavorite = { viewModel.dispatch(DashboardAction.AddFavoriteTag(it)) },
        onRemoveFavorite = { viewModel.dispatch(DashboardAction.RemoveFavoriteTag(it)) },
        onUpdateFavoriteName = { tag, name ->
            viewModel.dispatch(
                DashboardAction.UpdateFavoriteTagName(
                    tag,
                    name
                )
            )
        },
        onMoveFavorite = { from, to ->
            viewModel.dispatch(
                DashboardAction.MoveFavoriteTag(
                    from,
                    to
                )
            )
        },
        onAddCustomFavorite = { viewModel.dispatch(DashboardAction.AddCustomFavoriteTag(it)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    userProfileUiState: UserProfileUiState,
    lastReadHistory: DetailedHistory?,
    onCheckInClick: () -> Unit,
    onUpdateSlogan: (String) -> Unit,
    sloganResult: OperationResult?,
    onDismissSloganResult: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    passwordResult: OperationResult?,
    onDismissPasswordResult: () -> Unit,
    channelSettingsUiState: List<Channel>,
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
    favoriteTags: List<FavoriteTag>,
    onAddFavorite: (FavoriteTag) -> Unit,
    onRemoveFavorite: (FavoriteTag) -> Unit,
    onUpdateFavoriteName: (FavoriteTag, String) -> Unit,
    onMoveFavorite: (fromIndex: Int, toIndex: Int) -> Unit,
    onAddCustomFavorite: (String) -> Unit,
) {
    val drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ── 修改资料对话框 ────────────────────────────────────────────────────
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var inputSlogan by remember { mutableStateOf("") }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // sloganResult 驱动：成功时关闭对话框，失败时保持打开并显示错误
    val isSloganSaving = showEditProfileDialog && sloganResult == null &&
            (userProfileUiState is UserProfileUiState.Success)
    LaunchedEffect(sloganResult) {
        when (sloganResult) {
            OperationResult.Success -> {
                showEditProfileDialog = false
                onDismissSloganResult()
            }

            is OperationResult.Error, null -> Unit
        }
    }

    if (showEditProfileDialog) {
        // isSaving 由是否正在等待 sloganResult 推导：打开对话框且 sloganResult 还没回来时为 true
        var isSaving by remember { mutableStateOf(false) }
        // sloganResult 返回后重置 isSaving
        LaunchedEffect(sloganResult) {
            if (sloganResult != null) isSaving = false
        }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditProfileDialog = false },
            title = { Text("修改资料") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = inputSlogan,
                        onValueChange = { inputSlogan = it },
                        label = { Text("自我介绍") },
                        placeholder = { Text("输入您的个性签名") },
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (sloganResult is OperationResult.Error) {
                        Text(
                            text = sloganResult.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (isSaving) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    TextButton(
                        onClick = {
                            showEditProfileDialog = false
                            showChangePasswordDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("修改密码")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        onDismissSloganResult()
                        onUpdateSlogan(inputSlogan)
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        showEditProfileDialog = false
                        onDismissSloganResult()
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // ── 修改密码对话框 ────────────────────────────────────────────────────
    var inputOldPassword by remember { mutableStateOf("") }
    var inputNewPassword by remember { mutableStateOf("") }
    var inputConfirmPassword by remember { mutableStateOf("") }
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    // 本地校验错误（未发到服务端前）
    var localPasswordError by remember { mutableStateOf<String?>(null) }

    // passwordResult 驱动：成功时 Toast + 关闭，失败时保持打开
    LaunchedEffect(passwordResult) {
        when (passwordResult) {
            OperationResult.Success -> {
                showChangePasswordDialog = false
                android.widget.Toast.makeText(
                    context,
                    "密码修改成功",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                onDismissPasswordResult()
            }

            is OperationResult.Error, null -> Unit
        }
    }

    if (showChangePasswordDialog) {
        var isSaving by remember { mutableStateOf(false) }
        LaunchedEffect(passwordResult) {
            if (passwordResult != null) isSaving = false
        }
        // 打开时重置所有字段
        LaunchedEffect(Unit) {
            inputOldPassword = ""
            inputNewPassword = ""
            inputConfirmPassword = ""
            localPasswordError = null
            isSaving = false
            oldPasswordVisible = false
            newPasswordVisible = false
            confirmPasswordVisible = false
            onDismissPasswordResult()
        }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showChangePasswordDialog = false },
            title = { Text("修改密码") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = inputOldPassword,
                        onValueChange = { inputOldPassword = it },
                        label = { Text("旧密码") },
                        placeholder = { Text("请输入旧密码") },
                        singleLine = true,
                        enabled = !isSaving,
                        visualTransformation = if (oldPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                                Icon(
                                    imageVector = if (oldPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (oldPasswordVisible) "隐藏旧密码" else "显示旧密码"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    androidx.compose.material3.OutlinedTextField(
                        value = inputNewPassword,
                        onValueChange = { inputNewPassword = it },
                        label = { Text("新密码") },
                        placeholder = { Text("请输入新密码（至少8位）") },
                        singleLine = true,
                        enabled = !isSaving,
                        visualTransformation = if (newPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (newPasswordVisible) "隐藏新密码" else "显示新密码"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    androidx.compose.material3.OutlinedTextField(
                        value = inputConfirmPassword,
                        onValueChange = { inputConfirmPassword = it },
                        label = { Text("确认新密码") },
                        placeholder = { Text("请再次输入新密码") },
                        singleLine = true,
                        enabled = !isSaving,
                        visualTransformation = if (confirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "隐藏确认密码" else "显示确认密码"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 本地校验错误优先，服务端错误次之
                    val displayError = localPasswordError
                        ?: (passwordResult as? OperationResult.Error)?.message
                    if (displayError != null) {
                        Text(
                            text = displayError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (isSaving) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        localPasswordError = when {
                            inputOldPassword.isEmpty() -> "请输入旧密码"
                            inputNewPassword.isEmpty() -> "请输入新密码"
                            inputNewPassword.length < 8 -> "新密码长度至少需要8个字符"
                            inputNewPassword != inputConfirmPassword -> "两次输入的新密码不一致"
                            else -> null
                        }
                        if (localPasswordError != null) return@TextButton
                        isSaving = true
                        onDismissPasswordResult()
                        onChangePassword(inputOldPassword, inputNewPassword)
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        showChangePasswordDialog = false
                        onDismissPasswordResult()
                    }
                ) {
                    Text("取消")
                }
            }
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
                            navigationToReader(comicId, order)
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
                            // 同步初始化签名输入框，避免 LaunchedEffect 一帧延迟闪烁
                            if (userProfileUiState is UserProfileUiState.Success) {
                                inputSlogan = userProfileUiState.user.slogan
                            }
                            showEditProfileDialog = true
                        }
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
                        scope.launch {
                            drawerState.close()
                            onNotificationsClick()
                        }
                    },
                    onCommentsClick = {
                        scope.launch {
                            drawerState.close()
                            onCommentsClick()
                        }
                    },
                    onDownloadsClick = {
                        scope.launch {
                            drawerState.close()
                            onDownloadsClick()
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
                    onBookmarkClicked = { showBookmarkDrawer = true },
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
                lastReadHistory?.let { history ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        QuickResumeCard(
                            history = history,
                            onClick = navigationToReader,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

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
                            )
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
                    navigateToFavourite(action)
                },
                onAddFavorite = onAddFavorite,
                onRemoveFavorite = onRemoveFavorite,
                onUpdateName = onUpdateFavoriteName,
                onMove = onMoveFavorite,
                onAddCustom = onAddCustomFavorite,
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

private fun navigation(
    channel: Channel,
    context: Context,
    navigationToLeaderboard: () -> Unit,
    navigateToSearch: (DiscoveryAction) -> Unit,
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
//            "哔咔小程序" -> start(AppsActivity::class.java)
        "留言板" -> {
            android.widget.Toast.makeText(context, "该功能已下线", android.widget.Toast.LENGTH_SHORT).show()
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
    lastReadHistory: DetailedHistory?,
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
        when (userProfile) {
            is UserProfileUiState.Success -> UserProfileCard(
                state = userProfile,
                onCheckInClick = onCheckInClick,
                onEditProfile = onEditProfileClick,
            )

            else -> UserProfileStateCard(state = userProfile)
        }
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
            .testTag("dashboard:userProfile")
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // 无网络时显示离线缓存标识
                    if (state.isOfflineCache) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "离线",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

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
                    ),
                    modifier = Modifier.testTag("dashboard:checkIn"),
                ) {
                    Text(text = if (user.hasCheckedIn) "已打卡" else "打卡")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = user.gender,
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

@Composable
fun QuickResumeCard(
    history: DetailedHistory,
    onClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lastProgress = remember(history) {
        history.progressList.maxByOrNull { it.lastReadAt }
    }
    val chapterTitle = lastProgress?.let { "第 ${it.chapterId} 话" } ?: "第一话"
    val progressText = lastProgress?.let { "已读至第 ${it.currentPage} 页 / 共 ${it.pageCount} 页" } ?: "未开始阅读"
    val lastReadChapterOrder = lastProgress?.chapterId ?: 1

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