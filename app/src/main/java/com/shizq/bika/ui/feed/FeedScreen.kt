package com.shizq.bika.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.core.model.ComicSimple
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.model.Sort
import com.shizq.bika.core.ui.ComicCard
import com.shizq.bika.core.ui.ErrorState
import com.shizq.bika.core.ui.LoadingState
import com.shizq.bika.ui.tag.FilterChip
import com.shizq.bika.ui.tag.FilterGroup
import com.shizq.bika.ui.tag.FilterState
import com.shizq.bika.ui.tag.rememberFilterState
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.navigation.DiscoveryAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onBackClick: () -> Unit,
    onComicClick: (String) -> Unit,
    onNavigateToFeed: (DiscoveryAction) -> Unit = {},
    onBlockedTagsClick: () -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel(),
    title: String
) {
    val pagedComics = viewModel.pagedComics.collectAsLazyPagingItems()
    val detailedHistories by viewModel.detailedHistories.collectAsStateWithLifecycle()
    val currentSortOrder by viewModel.currentSortOrder.collectAsStateWithLifecycle()
    val filterSelections by viewModel.filterSelections.collectAsStateWithLifecycle()
    val excludeTopicsGlobal by viewModel.excludeTopicsGlobal.collectAsStateWithLifecycle()
    val favoriteTags by viewModel.favoriteTags.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()

    var showDrawer by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        FeedContent(
            title = title,
            pagedComics = pagedComics,
            detailedHistories = detailedHistories,
            onBackClick = onBackClick,
            onComicClick = onComicClick,
            currentSortOrder = currentSortOrder,
            onSortOrderChanged = viewModel::updateSortOrder,
            filterSelections = filterSelections,
            onFilterChanged = viewModel::toggleFilter,
            excludeTopicsGlobal = excludeTopicsGlobal,
            onExcludeTopicsGlobalChanged = viewModel::toggleExcludeTopicsGlobal,
            currentPage = currentPage,
            totalPages = totalPages,
            onPageChanged = viewModel::updatePage,
            onBookmarkClick = { showDrawer = true }
        )

        if (showDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showDrawer = false
                    }
            )
        }

        AnimatedVisibility(
            visible = showDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .align(Alignment.CenterEnd)
        ) {
            FavoriteTagsDrawer(
                favoriteTags = favoriteTags,
                currentAction = viewModel.currentAction,
                onNavigateToFeed = { action ->
                    showDrawer = false
                    onNavigateToFeed(action)
                },
                onAddFavorite = viewModel::addFavoriteTag,
                onRemoveFavorite = viewModel::removeFavoriteTag,
                onUpdateName = viewModel::updateFavoriteTagName,
                onMove = viewModel::moveFavoriteTag,
                onAddCustom = viewModel::addCustomFavoriteTag,
                onBlockedTagsClick = onBlockedTagsClick,
                onClose = { showDrawer = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedAppBar(
    title: String,
    currentSortOrder: Sort,
    onSortOrderChanged: (Sort) -> Unit,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回"
                )
            }
        },
        actions = {
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector = Icons.Rounded.Bookmarks,
                    contentDescription = "标签收藏夹"
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "排序"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                Sort.entries.fastForEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sort.title,
                                fontWeight = if (sort == currentSortOrder) FontWeight.Bold else FontWeight.Normal,
                                color = if (sort == currentSortOrder) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onSortOrderChanged(sort)
                        }
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FeedContent(
    title: String,
    pagedComics: LazyPagingItems<ComicSimple>,
    detailedHistories: List<DetailedHistory>,
    currentSortOrder: Sort,
    onSortOrderChanged: (Sort) -> Unit,
    onComicClick: (comicId: String) -> Unit,
    onBackClick: () -> Unit,
    filterSelections: Map<FilterGroup, List<String>>,
    onFilterChanged: (group: FilterGroup, value: String) -> Unit,
    excludeTopicsGlobal: Boolean,
    onExcludeTopicsGlobalChanged: (Boolean) -> Unit,
    currentPage: Int,
    totalPages: Int,
    onPageChanged: (Int) -> Unit,
    onBookmarkClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showPageJumpDialog by remember { mutableStateOf(false) }
    var pageJumpInputText by remember { mutableStateOf("") }
    var pageJumpInputError by remember { mutableStateOf<String?>(null) }

    val totalCount = pagedComics.itemCount

    // 动态计算当前可视项所属的页码
    val visiblePage by remember(currentPage, totalPages) {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            val computed = currentPage + (firstIndex / 40)
            computed.coerceIn(1, totalPages.coerceAtLeast(1))
        }
    }

    if (showPageJumpDialog) {
        // 页码校验+跳转逻辑，提取为局部函数避免重复
        fun handlePageJump() {
            val page = pageJumpInputText.trim().toIntOrNull()
            when {
                page == null -> pageJumpInputError = "请输入有效数字"
                page < 1 -> pageJumpInputError = "页码不能小于 1"
                page > totalPages -> pageJumpInputError = "页码不能超过 $totalPages"
                else -> {
                    onPageChanged(page)
                    scope.launch { listState.scrollToItem(0) }
                    showPageJumpDialog = false
                    pageJumpInputText = ""
                    pageJumpInputError = null
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                showPageJumpDialog = false
                pageJumpInputText = ""
                pageJumpInputError = null
            },
            title = { Text("跳转到指定页码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "当前共 $totalPages 页，请输入页码（1 ~ $totalPages）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pageJumpInputText,
                        onValueChange = {
                            pageJumpInputText = it
                            pageJumpInputError = null
                        },
                        label = { Text("页码") },
                        singleLine = true,
                        isError = pageJumpInputError != null,
                        supportingText = pageJumpInputError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { handlePageJump() }),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { handlePageJump() }) { Text("跳转") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPageJumpDialog = false
                        pageJumpInputText = ""
                        pageJumpInputError = null
                    }
                ) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            FeedAppBar(
                title = title,
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick,
                currentSortOrder = currentSortOrder,
                onSortOrderChanged = onSortOrderChanged,
                onBookmarkClick = onBookmarkClick
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val filterState = rememberFilterState(filterSelections)

            FilterRow(
                filterState = filterState,
                onFilterChanged = onFilterChanged,
                totalCount = totalCount,
                currentPage = visiblePage,
                totalPages = totalPages,
                onCountChipClick = { showPageJumpDialog = true },
                excludeTopicsGlobal = excludeTopicsGlobal,
                onExcludeTopicsGlobalChanged = onExcludeTopicsGlobalChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when (pagedComics.loadState.refresh) {
                is LoadState.Loading -> {
                    LoadingState(Modifier.weight(1f))
                }

                is LoadState.Error -> {
                    ErrorState(
                        onRetry = pagedComics::retry,
                        modifier = Modifier.weight(1f)
                    )
                }

                is LoadState.NotLoading -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f)
                    ) {
                        items(pagedComics.itemCount, key = pagedComics.itemKey { it.id }) { index ->
                            pagedComics[index]?.let { item ->
                                val enrichedItem = remember(item, detailedHistories) {
                                    item.injectSingleFrom(detailedHistories)
                                }
                                ComicCard(comic = enrichedItem) {
                                    onComicClick(item.id)
                                }
                            }
                        }

                        if (pagedComics.loadState.append is LoadState.Loading) {
                            item {
                                LoadingState(Modifier.wrapContentHeight())
                            }
                        }
                    }
                }
            }

            if (totalPages > 1 && pagedComics.loadState.refresh is LoadState.NotLoading) {
                PaginationBar(
                    currentPage = visiblePage,
                    totalPages = totalPages,
                    onPageChanged = { page ->
                        onPageChanged(page)
                        scope.launch { listState.scrollToItem(0) }
                    },
                    onPageIndicatorClick = { showPageJumpDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 8.dp, start = 16.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    filterState: FilterState,
    onFilterChanged: (group: FilterGroup, value: String) -> Unit,
    totalCount: Int,
    currentPage: Int,
    totalPages: Int,
    onCountChipClick: () -> Unit,
    excludeTopicsGlobal: Boolean,
    onExcludeTopicsGlobalChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(filterState.chips) { chipState ->
            FilterChip(
                state = chipState,
                onSelectionChanged = { value ->
                    chipState.kind?.let { group ->
                        onFilterChanged(group, value)
                    }
                },
                excludeTopicsGlobal = excludeTopicsGlobal,
                onExcludeTopicsGlobalChanged = onExcludeTopicsGlobalChanged
            )
        }

        item {
            SuggestionChip(
                onClick = onCountChipClick,
                enabled = totalCount > 0,
                label = {
                    Text(
                        text = if (totalCount > 0) {
                            if (totalPages > 1) "第 $currentPage / $totalPages 页" else "$totalCount 本"
                        } else {
                            "加载中…"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (totalCount > 0)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (totalCount > 0)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = totalCount > 0,
                    borderWidth = 0.dp,
                    borderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    onPageChanged: (Int) -> Unit,
    onPageIndicatorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = { onPageChanged(currentPage - 1) },
            enabled = currentPage > 1,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("上一页", style = MaterialTheme.typography.labelLarge)
        }

        Surface(
            onClick = onPageIndicatorClick,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Text(
                text = "$currentPage / $totalPages 页",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        FilledTonalButton(
            onClick = { onPageChanged(currentPage + 1) },
            enabled = currentPage < totalPages,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("下一页", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun FavoriteTagsDrawer(
    favoriteTags: List<FavoriteTag>,
    currentAction: DiscoveryAction? = null,
    onNavigateToFeed: (DiscoveryAction) -> Unit,
    onAddFavorite: (FavoriteTag) -> Unit,
    onRemoveFavorite: (FavoriteTag) -> Unit,
    onUpdateName: (FavoriteTag, String) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onAddCustom: (String) -> Unit,
    onBlockedTagsClick: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditMode by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var tagToRename by remember { mutableStateOf<FavoriteTag?>(null) }

    val currentTag = remember(currentAction) { currentAction?.toFavoriteTag() }
    val isCurrentFavorited = remember(favoriteTags, currentTag) {
        currentTag != null && favoriteTags.any { it.name == currentTag.name && it.actionType == currentTag.actionType }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "标签收藏夹",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onBlockedTagsClick) {
                    Icon(Icons.Rounded.Block, contentDescription = "标签屏蔽管理")
                }
                IconButton(onClick = { showAddCustomDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "新增标签")
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭")
                }
            }

            // Quick Add Current Tag
            if (currentTag != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isCurrentFavorited) {
                        Button(
                            onClick = { onRemoveFavorite(currentTag) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Star, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("已收藏当前标签 (点击取消)")
                        }
                    } else {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { onAddFavorite(currentTag) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.StarBorder, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("收藏当前标签")
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // List Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的收藏",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { isEditMode = !isEditMode },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(if (isEditMode) "完成" else "编辑")
                }
            }

            // Tags List
            if (favoriteTags.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无收藏，点击上方按钮收藏",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(favoriteTags) { index, tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isEditMode) {
                                    onNavigateToFeed(tag.toAction())
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEditMode) {
                                IconButton(
                                    onClick = { onRemoveFavorite(tag) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Bookmarks,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                            }

                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (isEditMode) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { tagToRename = tag },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "编辑名称",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMove(index, index - 1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowUp,
                                            contentDescription = "上移"
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMove(index, index + 1) },
                                        enabled = index < favoriteTags.size - 1,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "下移"
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }

    if (showAddCustomDialog) {
        var nameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = { Text("新增自定义标签") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            onAddCustom(nameInput.trim())
                            showAddCustomDialog = false
                        }
                    }
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (tagToRename != null) {
        var renameInput by remember { mutableStateOf(tagToRename?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { tagToRename = null },
            title = { Text("重命名标签") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("新名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank() && tagToRename != null) {
                            onUpdateName(tagToRename!!, renameInput.trim())
                            tagToRename = null
                        }
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToRename = null }) {
                    Text("取消")
                }
            }
        )
    }
}