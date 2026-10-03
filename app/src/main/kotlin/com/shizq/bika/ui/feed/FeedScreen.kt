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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.shizq.bika.R
import com.shizq.bika.core.database.model.DetailedHistory
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
import com.shizq.bika.util.injectFromHistoryMap

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
            FavoriteTagsDrawer(
                items = state.favoriteTags.map(FavoriteTag::toUiItem),
                currentTag = currentTag,
                onNavigateToFeed = { action ->
                    favoriteSheetVisible = false
                    onNavigate(FeedDestination.Feed(action))
                },
                onAddFavorite = { onAction(FeedAction.AddFavorite(it)) },
                onRemoveFavorite = { onAction(FeedAction.RemoveFavorite(it)) },
                onRenameRequest = { onNavigate(FeedDestination.RenameFavorite(it)) },
                onMove = { from, to -> onAction(FeedAction.MoveFavorite(from, to)) },
                onAddCustomRequest = { onNavigate(FeedDestination.AddFavorite) },
                onBlockedTagsClick = { onNavigate(FeedDestination.BlockedTags) },
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
            IconButton(onClick = onBackClick) {
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
    val success = state.content as? FeedContentState.Success
    val page = success?.page
    val displayedContentKey = success?.let {
        DisplayedContentKey(
            page = it.page.page,
            sort = it.displayedQuery.sort,
            itemIds = it.page.items.map { comic -> comic.id },
        )
    }
    val refreshState = success?.refreshState ?: FeedRefreshState.Idle
    val isRefreshing = refreshState is FeedRefreshState.Loading
    val filterState = rememberFilterState(state.filterSelections)
    val historyMap = remember(state.detailedHistories) {
        state.detailedHistories.associateBy { it.history.id }
    }

    // Only a successfully displayed page changes the list position. A failed request keeps
    // both the old content and the user's reading position.
    LaunchedEffect(displayedContentKey) {
        if (displayedContentKey != null) listState.scrollToItem(0)
    }

    Scaffold(
        topBar = {
            FeedAppBar(
                title = title,
                scrollBehavior = scrollBehavior,
                onBackClick = { onNavigate(FeedDestination.Back) },
                currentSortOrder = success?.displayedQuery?.sort ?: state.query.sort,
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
                .padding(innerPadding),
        ) {
            stickyHeader(key = "feed-filter-header") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                ) {
                    Column {
                        if (isRefreshing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        FilterRow(
                            chips = filterState.chips,
                            onFilterChanged = { group, option ->
                                onAction(FeedAction.ToggleFilter(group, option))
                            },
                            summary = feedSummary(state.content),
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
                content = state.content,
                historyMap = historyMap,
                onRetry = { onAction(FeedAction.Retry) },
                onComicClick = { onNavigate(FeedDestination.Comic(it)) },
            )

            if (page != null && page.totalPages > 1) {
                item(key = "feed-pagination", contentType = "pagination") {
                    PaginationBar(
                        currentPage = page.page,
                        totalPages = page.totalPages,
                        enabled = !isRefreshing,
                        onPageChanged = { onAction(FeedAction.ChangePage(it)) },
                        onPageIndicatorClick = {
                            onNavigate(
                                FeedDestination.PageJump(
                                    currentPage = page.page,
                                    totalPages = page.totalPages,
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

private data class DisplayedContentKey(
    val page: Int,
    val sort: SortOrder,
    val itemIds: List<String>,
)

private fun LazyListScope.feedContentItems(
    content: FeedContentState,
    historyMap: Map<String, DetailedHistory>,
    onRetry: () -> Unit,
    onComicClick: (String) -> Unit,
) {
    when (content) {
        FeedContentState.Initial,
        FeedContentState.Loading -> item(key = "feed-initial-loading", contentType = "loading") {
            LoadingState(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )
        }

        is FeedContentState.Error -> item(key = "feed-initial-error", contentType = "error") {
            ErrorState(
                onRetry = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            )
        }

        is FeedContentState.Success -> {
            if (content.refreshState is FeedRefreshState.Failed) {
                item(key = "feed-refresh-error", contentType = "error-banner") {
                    RefreshErrorBanner(onRetry = onRetry)
                }
            }

            if (content.page.items.isEmpty()) {
                item(key = "feed-empty", contentType = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.feed_empty_current_page),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
private fun feedSummary(content: FeedContentState): String {
    val isClientFiltered = content is FeedContentState.Success && content.isClientFiltered
    return when (content) {
        FeedContentState.Initial,
        FeedContentState.Loading -> stringResource(R.string.feed_loading)

        is FeedContentState.Error -> stringResource(R.string.feed_load_failed)
        is FeedContentState.Success -> {
            val page = content.page
            when {
                isClientFiltered -> stringResource(
                    R.string.feed_page_visible_count,
                    page.page,
                    page.totalPages,
                    page.items.size,
                )

                page.totalCount != null -> stringResource(
                    R.string.feed_total_count,
                    page.totalCount
                )

                else -> stringResource(R.string.feed_page_indicator, page.page, page.totalPages)
            }
        }
    }
}

@Composable
private fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    enabled: Boolean,
    onPageChanged: (Int) -> Unit,
    onPageIndicatorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onPageChanged(currentPage - 1) },
            enabled = enabled && currentPage > 1,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                contentDescription = stringResource(R.string.feed_previous_page),
            )
        }

        Surface(
            onClick = onPageIndicatorClick,
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Text(
                text = stringResource(R.string.feed_page_indicator, currentPage, totalPages),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        IconButton(
            onClick = { onPageChanged(currentPage + 1) },
            enabled = enabled && currentPage < totalPages,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                contentDescription = stringResource(R.string.feed_next_page),
            )
        }
    }
}
