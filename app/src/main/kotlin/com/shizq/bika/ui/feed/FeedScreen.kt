package com.shizq.bika.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder

import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.domain.filter.FilterGroup
import com.shizq.bika.core.domain.filter.FilterOption
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.ui.ComicCard
import com.shizq.bika.core.ui.ErrorState
import com.shizq.bika.core.ui.LoadingState
import com.shizq.bika.navigation.AddFavoriteTagResult
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.navigation.FeedPageJumpResult
import com.shizq.bika.navigation.RenameFavoriteTagResult
import com.shizq.bika.ui.tag.FilterChip
import com.shizq.bika.ui.tag.FilterChipState
import com.shizq.bika.util.injectFromHistoryMap

sealed interface FeedDestination {
    data object Back : FeedDestination
    data class Comic(val id: String) : FeedDestination
    data class Feed(val action: DiscoveryAction) : FeedDestination
    data object BlockedTags : FeedDestination
    data class PageJump(val currentPage: Int, val totalPages: Int) : FeedDestination
    data object AddFavorite : FeedDestination
    data class RenameFavorite(val tag: FavoriteTag) : FeedDestination
}

@Composable
fun FeedRoute(
    title: String,
    viewModel: FeedViewModel,
    onNavigate: (FeedDestination) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ResultEffect<AddFavoriteTagResult> { result ->
        viewModel.dispatch(FeedAction.AddCustomFavorite(result.name))
    }
    ResultEffect<RenameFavoriteTagResult> { result ->
        viewModel.dispatch(FeedAction.RenameFavorite(result.tag, result.name))
    }
    ResultEffect<FeedPageJumpResult> { result ->
        viewModel.dispatch(FeedAction.ChangePage(result.page))
    }

    FeedScreen(
        title = title,
        state = state,
        currentTag = viewModel.currentAction.toFavoriteTag(),
        onAction = viewModel::dispatch,
        onNavigate = onNavigate,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    title: String,
    state: FeedUiState,
    currentTag: FavoriteTag?,
    onAction: (FeedAction) -> Unit,
    onNavigate: (FeedDestination) -> Unit,
) {
    var drawerVisible by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        FeedContent(
            title = title,
            state = state,
            onAction = onAction,
            onNavigate = onNavigate,
            onBookmarkClick = { drawerVisible = true },
        )

        if (drawerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { drawerVisible = false },
            )
        }

        AnimatedVisibility(
            visible = drawerVisible,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .align(Alignment.CenterEnd),
        ) {
            FavoriteTagsDrawer(
                items = state.favoriteTags.map(FavoriteTag::toUiItem),
                currentTag = currentTag,
                onNavigateToFeed = { action ->
                    drawerVisible = false
                    onNavigate(FeedDestination.Feed(action))
                },
                onAddFavorite = { onAction(FeedAction.AddFavorite(it)) },
                onRemoveFavorite = { onAction(FeedAction.RemoveFavorite(it)) },
                onRenameRequest = { onNavigate(FeedDestination.RenameFavorite(it)) },
                onMove = { from, to -> onAction(FeedAction.MoveFavorite(from, to)) },
                onAddCustomRequest = { onNavigate(FeedDestination.AddFavorite) },
                onBlockedTagsClick = { onNavigate(FeedDestination.BlockedTags) },
                onClose = { drawerVisible = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedAppBar(
    title: String,
    currentSortOrder: SortOrder,
    onSortOrderChanged: (SortOrder) -> Unit,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
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
                SortOrder.entries.fastForEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sort.label,
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
    state: FeedUiState,
    onAction: (FeedAction) -> Unit,
    onNavigate: (FeedDestination) -> Unit,
    onBookmarkClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val page = (state.content as? FeedContentState.Success)?.page
    val currentPage = page?.page ?: state.query.page
    val totalPages = page?.totalPages ?: 1
    val totalCount = page?.totalCount ?: page?.items?.size ?: 0

    LaunchedEffect(state.query.page) {
        listState.scrollToItem(0)
    }

    Scaffold(
        topBar = {
            FeedAppBar(
                title = title,
                scrollBehavior = scrollBehavior,
                onBackClick = { onNavigate(FeedDestination.Back) },
                currentSortOrder = state.query.sort,
                onSortOrderChanged = { onAction(FeedAction.ChangeSort(it)) },
                onBookmarkClick = onBookmarkClick,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val filterChips = remember(state.filterSelections) {
                FilterGroup.all.map { group ->
                    val selected = state.filterSelections[group].orEmpty()
                    FilterChipState(
                        group = group,
                        options = group.options + selected.filterNot { it in group.options },
                        selected = selected,
                    )
                }
            }

            FilterRow(
                chips = filterChips,
                onFilterChanged = { group, option ->
                    onAction(FeedAction.ToggleFilter(group, option))
                },
                totalCount = totalCount,
                currentPage = currentPage,
                totalPages = totalPages,
                onCountChipClick = {
                    onNavigate(FeedDestination.PageJump(currentPage, totalPages))
                },
                excludeTopicsGlobal = state.excludeTopicsGlobal,
                onExcludeTopicsGlobalChanged = {
                    onAction(FeedAction.SetGlobalTopicFilter(it))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            FeedBody(
                content = state.content,
                histories = state.detailedHistories,
                listState = listState,
                onRetry = { onAction(FeedAction.Retry) },
                onComicClick = { onNavigate(FeedDestination.Comic(it)) },
                modifier = Modifier.weight(1f),
            )

            if (page != null && totalPages > 1) {
                PaginationBar(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPageChanged = { onAction(FeedAction.ChangePage(it)) },
                    onPageIndicatorClick = {
                        onNavigate(FeedDestination.PageJump(currentPage, totalPages))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun FeedBody(
    content: FeedContentState,
    histories: List<DetailedHistory>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRetry: () -> Unit,
    onComicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (content) {
        FeedContentState.Initial,
        FeedContentState.Loading -> LoadingState(modifier)

        is FeedContentState.Error -> ErrorState(onRetry = onRetry, modifier = modifier)
        is FeedContentState.Success -> {
            if (content.page.items.isEmpty()) {
                Box(modifier = modifier, contentAlignment = Alignment.Center) {
                    Text(
                        text = "没有符合条件的内容",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                FeedList(
                    content = content,
                    histories = histories,
                    listState = listState,
                    onRetry = onRetry,
                    onComicClick = onComicClick,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun FeedList(
    content: FeedContentState.Success,
    histories: List<DetailedHistory>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRetry: () -> Unit,
    onComicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val historyMap = remember(histories) {
        histories.associateBy { it.history.id }
    }

    Column(modifier = modifier) {
        if (content.refreshError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("刷新失败，当前显示上次数据", modifier = Modifier.weight(1f))
                    TextButton(onClick = onRetry) { Text("重试") }
                }
            }
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(
                items = content.page.items,
                key = { comic -> comic.id },
                contentType = { "comic" },
            ) { comic ->
                val enrichedComic = remember(comic, historyMap) {
                    comic.injectFromHistoryMap(historyMap)
                }
                ComicCard(comic = enrichedComic) { onComicClick(comic.id) }
            }

            if (content.isRefreshing) {
                item(key = "feed-loading", contentType = "loading") {
                    LoadingState(Modifier.wrapContentHeight())
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    chips: List<FilterChipState>,
    onFilterChanged: (group: FilterGroup, option: FilterOption) -> Unit,
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
        items(
            items = chips,
            key = { it.group.label },
        ) { chipState ->
            FilterChip(
                state = chipState,
                onSelectionChanged = { option -> onFilterChanged(chipState.group, option) },
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

data class FavoriteTagUiItem(
    val stableKey: String,
    val tag: FavoriteTag,
    val action: DiscoveryAction?,
)

fun FavoriteTag.toUiItem() = FavoriteTagUiItem(
    stableKey = "$actionType:$actionId:$name",
    tag = this,
    action = toAction(),
)

@Composable
fun FavoriteTagsDrawer(
    items: List<FavoriteTagUiItem>,
    currentTag: FavoriteTag? = null,
    onNavigateToFeed: (DiscoveryAction) -> Unit,
    onAddFavorite: (FavoriteTag) -> Unit,
    onRemoveFavorite: (FavoriteTag) -> Unit,
    onRenameRequest: (FavoriteTag) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onAddCustomRequest: () -> Unit,
    onBlockedTagsClick: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditMode by rememberSaveable { mutableStateOf(false) }

    val isCurrentFavorited = remember(items, currentTag) {
        currentTag != null && items.any { it.tag.isSameTag(currentTag) }
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
                IconButton(onClick = onAddCustomRequest) {
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
            if (items.isEmpty()) {
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
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.stableKey },
                    ) { index, item ->
                        FavoriteTagRow(
                            item = item,
                            index = index,
                            itemCount = items.size,
                            isEditMode = isEditMode,
                            onNavigateToFeed = onNavigateToFeed,
                            onRemoveFavorite = onRemoveFavorite,
                            onRenameRequest = onRenameRequest,
                            onMove = onMove,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteTagRow(
    item: FavoriteTagUiItem,
    index: Int,
    itemCount: Int,
    isEditMode: Boolean,
    onNavigateToFeed: (DiscoveryAction) -> Unit,
    onRemoveFavorite: (FavoriteTag) -> Unit,
    onRenameRequest: (FavoriteTag) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isEditMode && item.action != null) {
                item.action?.let(onNavigateToFeed)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isEditMode) {
            IconButton(
                onClick = { onRemoveFavorite(item.tag) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.width(8.dp))
        } else {
            Icon(
                imageVector = Icons.Rounded.Bookmarks,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.tag.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.action == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.action == null) {
                Text(
                    text = "无法打开，建议删除",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (isEditMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onRenameRequest(item.tag) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "编辑名称",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = { onMove(index, index - 1) },
                    enabled = index > 0,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "上移")
                }
                IconButton(
                    onClick = { onMove(index, index + 1) },
                    enabled = index < itemCount - 1,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "下移")
                }
            }
        }
    }
}