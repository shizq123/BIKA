package com.shizq.bika.ui.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import kotlinx.coroutines.flow.distinctUntilChanged
import com.shizq.bika.R
import com.shizq.bika.core.domain.filter.FilterGroup
import com.shizq.bika.core.domain.filter.FilterOption
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.ui.ComicCard
import com.shizq.bika.core.ui.ErrorState
import com.shizq.bika.core.ui.LoadingState
import com.shizq.bika.navigation.AddFavoriteTagResult
import com.shizq.bika.navigation.FeedPageJumpResult
import com.shizq.bika.navigation.RenameFavoriteTagResult
import com.shizq.bika.ui.tag.FilterChip
import com.shizq.bika.ui.tag.FilterChipState
import com.shizq.bika.ui.tag.rememberFilterState

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
    var favoriteSheetVisible by remember { mutableStateOf(false) }

    FeedContent(
        title = title,
        state = state,
        onAction = onAction,
        onNavigate = onNavigate,
        onBookmarkClick = { favoriteSheetVisible = true },
    )

    if (favoriteSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { favoriteSheetVisible = false },
        ) {
            FavoriteTagsContent(
                items = state.favoriteTags.map(FavoriteTag::toUiItem),
                currentTag = currentTag,
                onNavigateToFeed = { action ->
                    favoriteSheetVisible = false
                    onNavigate(FeedDestination.Feed(action))
                },
                onAddFavorite = { onAction(FeedAction.AddFavorite(it)) },
                onRemoveFavorite = { onAction(FeedAction.RemoveFavorite(it)) },
                onRenameRequest = {
                    favoriteSheetVisible = false
                    onNavigate(FeedDestination.RenameFavorite(it))
                },
                onMove = { from, to -> onAction(FeedAction.MoveFavorite(from, to)) },
                onAddCustomRequest = {
                    favoriteSheetVisible = false
                    onNavigate(FeedDestination.AddFavorite)
                },
                onBlockedTagsClick = {
                    favoriteSheetVisible = false
                    onNavigate(FeedDestination.BlockedTags)
                },
                onClose = { favoriteSheetVisible = false },
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
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag(FeedTestTags.Back),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.feed_action_back),
                )
            }
        },
        actions = {
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector = Icons.Rounded.Bookmarks,
                    contentDescription = stringResource(R.string.feed_action_favorite_tags),
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.feed_action_sort),
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                SortOrder.entries.fastForEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sort.label,
                                fontWeight = if (sort == currentSortOrder) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                                color = if (sort == currentSortOrder) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Unspecified
                                },
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onSortOrderChanged(sort)
                        },
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FeedContent(
    title: String,
    state: FeedUiState,
    onAction: (FeedAction) -> Unit,
    onNavigate: (FeedDestination) -> Unit,
    onBookmarkClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val content = state as? FeedUiState.Content
    val pagination = state.toPaginationUiState()
    val displayedContentKey = content?.let {
        DisplayedContentKey(
            page = it.page.page,
            sort = it.displayedQuery.sort,
            itemIds = it.page.items.map { comic -> comic.id },
        )
    }
    val refreshState = content?.refreshState ?: FeedRefreshState.Idle
    val isRefreshing = refreshState is FeedRefreshState.Loading
    val filterState = rememberFilterState(state.filterSelections)

    var lastPage by remember { mutableStateOf<Int?>(null) }
    var lastSort by remember { mutableStateOf<SortOrder?>(null) }
    var lastFirstItemId by remember { mutableStateOf<String?>(null) }

    // 只有非追加的成功页面加载（如切换页码、切换排序或筛选导致首项变动）才重置列表位置到顶部。
    LaunchedEffect(displayedContentKey) {
        if (displayedContentKey != null) {
            val currentFirstItemId = content.page.items.firstOrNull()?.id
            val isContinuous = state.continuousScrollEnabled
            val isAppending = isContinuous &&
                    lastPage != null &&
                    displayedContentKey.page > lastPage!! &&
                    displayedContentKey.sort == lastSort &&
                    currentFirstItemId == lastFirstItemId

            if (!isAppending) {
                listState.scrollToItem(0)
            }
            lastPage = displayedContentKey.page
            lastSort = displayedContentKey.sort
            lastFirstItemId = currentFirstItemId
        }
    }

    if (state.continuousScrollEnabled && content != null) {
        val totalPages = content.page.totalPages
        val currentPage = content.page.page
        val isAppendingOrLoading = content.refreshState is FeedRefreshState.Loading ||
                content.refreshState is FeedRefreshState.Appending

        LaunchedEffect(listState, currentPage, totalPages, isAppendingOrLoading) {
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisibleIndex >= totalItems - 3
            }.distinctUntilChanged()
            .collect { shouldAppend ->
                if (shouldAppend && currentPage < totalPages && !isAppendingOrLoading) {
                    onAction(FeedAction.AppendNextPage)
                }
            }
        }
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(FeedTestTags.List),
        ) {
            stickyHeader(key = "feed-filter-header") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                ) {
                    Column {
                        if (isRefreshing) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(FeedTestTags.RefreshProgress),
                            )
                        }
                        FilterRow(
                            chips = filterState.chips,
                            onFilterChanged = { group, option ->
                                onAction(FeedAction.ToggleFilter(group, option))
                            },
                            summary = feedSummary(state),
                            excludeTopicsGlobal = state.excludeTopicsGlobal,
                            onExcludeTopicsGlobalChanged = {
                                onAction(FeedAction.SetGlobalTopicFilter(it))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            feedContentItems(
                state = state,
                onRetry = { onAction(FeedAction.Retry) },
                onComicClick = { onNavigate(FeedDestination.Comic(it)) },
            )

            if (state.continuousScrollEnabled) {
                if (content != null && content.page.items.isNotEmpty()) {
                    when (content.refreshState) {
                        is FeedRefreshState.Appending -> {
                            item(key = "feed-append-loading", contentType = "append-status") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .testTag(FeedTestTags.AppendLoading),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                        )
                                        Text(
                                            text = stringResource(R.string.feed_append_loading),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        is FeedRefreshState.Failed -> {
                            item(key = "feed-append-failed", contentType = "append-status") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .testTag(FeedTestTags.AppendFailed),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.feed_append_failed),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                        TextButton(onClick = { onAction(FeedAction.AppendNextPage) }) {
                                            Text(stringResource(R.string.feed_retry))
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            if (content.page.page >= content.page.totalPages) {
                                item(key = "feed-append-end", contentType = "append-status") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp)
                                            .testTag(FeedTestTags.AppendEnd),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.feed_append_no_more),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (pagination != null) {
                    item(key = "feed-pagination", contentType = "pagination") {
                        PaginationBar(
                            state = pagination,
                            onPageChanged = { onAction(FeedAction.ChangePage(it)) },
                            onPageIndicatorClick = {
                                onNavigate(
                                    FeedDestination.PageJump(
                                        currentPage = pagination.currentPage,
                                        totalPages = pagination.totalPages,
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private data class DisplayedContentKey(
    val page: Int,
    val sort: SortOrder,
    val itemIds: List<String>,
)

private fun LazyListScope.feedContentItems(
    state: FeedUiState,
    onRetry: () -> Unit,
    onComicClick: (String) -> Unit,
) {
    when (state) {
        is FeedUiState.Initial -> item(key = "feed-initial-loading", contentType = "loading") {
            LoadingState(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .testTag(FeedTestTags.InitialLoading)
            )
        }

        is FeedUiState.Error -> item(key = "feed-initial-error", contentType = "error") {
            ErrorState(
                onRetry = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .testTag(FeedTestTags.InitialError),
            )
        }

        is FeedUiState.Content -> {
            if (state.refreshState is FeedRefreshState.Failed &&
                (!state.continuousScrollEnabled || state.refreshState.targetQuery.page <= 1)
            ) {
                item(key = "feed-refresh-error", contentType = "error-banner") {
                    RefreshErrorBanner(onRetry = onRetry)
                }
            }

            if (state.page.items.isEmpty()) {
                item(key = "feed-empty", contentType = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .testTag(FeedTestTags.Empty),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(
                                if (state.isClientFiltered) {
                                    R.string.feed_empty_current_page
                                } else {
                                    R.string.feed_empty
                                }
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
            } else {
                items(
                    items = state.page.items,
                    key = { comic -> comic.id },
                    contentType = { "comic" },
                ) { comic ->
                    ComicCard(comic = comic) { onComicClick(comic.id) }
                }
            }
        }
    }
}

@Composable
private fun RefreshErrorBanner(onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag(FeedTestTags.RefreshError),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.feed_refresh_failed),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.feed_retry))
            }
        }
    }
}

@Composable
private fun FilterRow(
    chips: List<FilterChipState>,
    onFilterChanged: (group: FilterGroup, option: FilterOption) -> Unit,
    summary: String,
    excludeTopicsGlobal: Boolean,
    onExcludeTopicsGlobalChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
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
                onExcludeTopicsGlobalChanged = onExcludeTopicsGlobalChanged,
            )
        }

        item(key = "feed-summary") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun feedSummary(state: FeedUiState): String =
    when (val summary = state.toSummaryUiState()) {
        FeedSummaryUiState.Loading -> stringResource(R.string.feed_loading)
        FeedSummaryUiState.LoadFailed -> stringResource(R.string.feed_load_failed)
        FeedSummaryUiState.Refreshing -> stringResource(R.string.feed_refreshing)
        is FeedSummaryUiState.LoadingPage -> stringResource(
            R.string.feed_loading_page,
            summary.page,
        )

        is FeedSummaryUiState.VisibleOnPage -> stringResource(
            R.string.feed_page_visible_count,
            summary.page,
            summary.totalPages,
            summary.visibleCount,
        )

        is FeedSummaryUiState.TotalCount -> stringResource(
            R.string.feed_total_count,
            summary.count,
        )

        is FeedSummaryUiState.PageIndicator -> stringResource(
            R.string.feed_page_indicator,
            summary.page,
            summary.totalPages,
        )
}

@Composable
private fun PaginationBar(
    state: FeedPaginationUiState,
    onPageChanged: (Int) -> Unit,
    onPageIndicatorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.testTag(FeedTestTags.Pagination),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onPageChanged(state.currentPage - 1) },
            enabled = state.previousEnabled,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                contentDescription = stringResource(R.string.feed_previous_page),
            )
        }

        Surface(
            onClick = onPageIndicatorClick,
            enabled = state.indicatorEnabled,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Text(
                text = stringResource(
                    R.string.feed_page_indicator,
                    state.currentPage,
                    state.totalPages,
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        IconButton(
            onClick = { onPageChanged(state.currentPage + 1) },
            enabled = state.nextEnabled,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                contentDescription = stringResource(R.string.feed_next_page),
            )
        }
    }
}

internal object FeedTestTags {
    const val Back = "feed-back"
    const val List = "feed-list"
    const val InitialLoading = "feed-initial-loading"
    const val InitialError = "feed-initial-error"
    const val RefreshProgress = "feed-refresh-progress"
    const val RefreshError = "feed-refresh-error"
    const val Empty = "feed-empty"
    const val Pagination = "feed-pagination"
    const val AppendLoading = "feed-append-loading"
    const val AppendFailed = "feed-append-failed"
    const val AppendEnd = "feed-append-end"
}
