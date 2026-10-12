package com.shizq.bika.ui.feed

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.R
import com.shizq.bika.core.domain.filter.FeedCriteria
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.ui.ComicCard
import com.shizq.bika.core.ui.ComicSharedElement
import com.shizq.bika.core.ui.ComicSharedElementKey
import com.shizq.bika.core.ui.ErrorState
import com.shizq.bika.core.ui.LoadingState
import com.shizq.bika.navigation.AddFavoriteTagResult
import com.shizq.bika.navigation.RenameFavoriteTagResult
import com.shizq.bika.ui.tag.FeedFilterChange
import com.shizq.bika.ui.tag.FilterChip
import com.shizq.bika.ui.tag.FilterChipState
import com.shizq.bika.ui.tag.rememberFilterState
import kotlin.uuid.Uuid

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedRoute(
    title: String,
    viewModel: FeedViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigate: (FeedDestination) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val comics = viewModel.comics.collectAsLazyPagingItems()

    ResultEffect<AddFavoriteTagResult> { result ->
        viewModel.dispatch(FeedAction.AddCustomFavorite(result.name))
    }
    ResultEffect<RenameFavoriteTagResult> { result ->
        viewModel.dispatch(FeedAction.RenameFavorite(result.tag, result.name))
    }

    FeedScreen(
        title = title,
        state = state,
        comics = comics,
        currentTag = viewModel.currentAction.toFavoriteTag(),
        onAction = viewModel::dispatch,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onNavigate = onNavigate,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FeedScreen(
    title: String,
    state: FeedUiState,
    comics: LazyPagingItems<ComicSummary>,
    currentTag: FavoriteTag?,
    onAction: (FeedAction) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigate: (FeedDestination) -> Unit,
) {
    var favoriteSheetVisible by remember { mutableStateOf(false) }

    FeedContent(
        title = title,
        state = state,
        comics = comics,
        onAction = onAction,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onNavigate = onNavigate,
        onBookmarkClick = { favoriteSheetVisible = true },
    )
    // TODO: 使用navigation 
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
    onRefreshClick: () -> Unit,
    refreshEnabled: Boolean,
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
            IconButton(
                onClick = onRefreshClick,
                enabled = refreshEnabled,
                modifier = Modifier.testTag(FeedTestTags.Refresh),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.feed_action_refresh),
                )
            }
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

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalSharedTransitionApi::class,
)
@Composable
private fun FeedContent(
    title: String,
    state: FeedUiState,
    comics: LazyPagingItems<ComicSummary>,
    onAction: (FeedAction) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigate: (FeedDestination) -> Unit,
    onBookmarkClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val refreshLoadState = comics.loadState.refresh
    val isRefreshing = refreshLoadState is LoadState.Loading && comics.itemCount > 0
    val filterState = rememberFilterState(state.query.criteria)

    // TODO: 可以考虑移除？ 
    val displayedContentKey = remember(state) {
        DisplayedContentKey(
            sort = state.query.sort,
            criteria = state.query.criteria,
        )
    }

    LaunchedEffect(displayedContentKey) {
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
                onRefreshClick = comics::refresh,
                refreshEnabled = state.preferencesReady &&
                        refreshLoadState !is LoadState.Loading,

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
                        FilterHeader(
                            chips = filterState.chips,
                            onFilterChanged = { change ->
                                onAction(change.toFeedAction())
                            },
                            summary = feedSummary(
                                state = state,
                                itemCount = comics.itemCount,
                                refreshLoadState = refreshLoadState,
                            ),
                            currentSortOrder = state.query.sort,
                            excludeTopicsGlobal = state.excludeTopicsGlobal,
                            onExcludeTopicsGlobalChanged = {
                                onAction(FeedAction.SetGlobalTopicFilter(it))
                            },
                            onClearFilters = { onAction(FeedAction.ClearFilters) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            feedContentItems(
                comics = comics,
                metadataLoading = !state.preferencesReady,

                isClientFiltered = state.query.criteria.hasAnySelection ||
                        state.data.globalPolicy.blockedTopicsEnabled ||
                        state.data.globalPolicy.blockedTags.isNotEmpty(),

                onRetry = comics::retry,
                canClearFilters = state.query.criteria.hasAnySelection,

                onClearFilters = { onAction(FeedAction.ClearFilters) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onComicClick = { comic, transitionId ->
                    onNavigate(
                        FeedDestination.Comic(
                            id = comic.id,
                            coverUrl = comic.image.originalImageUrl,
                            title = comic.title,
                            transitionId = transitionId,
                        )
                    )
                },
            )
        }
    }
}

private data class DisplayedContentKey(
    val sort: SortOrder,
    val criteria: FeedCriteria,
)

private fun FeedFilterChange.toFeedAction(): FeedAction = when (this) {
    is FeedFilterChange.IncludedTopic -> FeedAction.ToggleIncludedTopic(topic)
    is FeedFilterChange.ExcludedTopic -> FeedAction.ToggleExcludedTopic(topic)
    is FeedFilterChange.Status -> FeedAction.ToggleStatus(status)
    is FeedFilterChange.EpisodeRange -> FeedAction.ToggleEpisodeRange(range)
    is FeedFilterChange.PageRange -> FeedAction.TogglePageRange(range)
}

@OptIn(ExperimentalSharedTransitionApi::class)
private fun LazyListScope.feedContentItems(
    comics: LazyPagingItems<ComicSummary>,
    metadataLoading: Boolean,
    isClientFiltered: Boolean,
    onRetry: () -> Unit,
    canClearFilters: Boolean,
    onClearFilters: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onComicClick: (ComicSummary, String) -> Unit,
) {
    val refresh = comics.loadState.refresh
    when {
        metadataLoading || (refresh is LoadState.Loading && comics.itemCount == 0) -> {
            item(key = "feed-initial-loading", contentType = "loading") {
                LoadingState(
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .testTag(FeedTestTags.InitialLoading)
                )
            }
        }

        refresh is LoadState.Error && comics.itemCount == 0 -> {
            item(key = "feed-initial-error", contentType = "error") {
                ErrorState(
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .testTag(FeedTestTags.InitialError),
                )
            }
        }

        else -> {
            if (refresh is LoadState.Error) {
                item(key = "feed-refresh-error", contentType = "error-banner") {
                    RefreshErrorBanner(onRetry = onRetry)
                }
            }

            if (comics.itemCount == 0) {
                item(key = "feed-empty", contentType = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .testTag(FeedTestTags.Empty),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (isClientFiltered) {
                                        R.string.feed_empty_current_page
                                    } else {
                                        R.string.feed_empty
                                    }
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                            if (canClearFilters) {
                                TextButton(onClick = onClearFilters) {
                                    Text(stringResource(R.string.feed_empty_clear_filters))
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    count = comics.itemCount,
                    // Some endpoints can return duplicate ids across pages; include the index.
                    key = { index -> "${index}_${comics.peek(index)?.id ?: "placeholder"}" },
                    contentType = { "comic" },
                ) { index ->
                    comics[index]?.let { comic ->
                        val transitionId = rememberSaveable(comic.id, index) {
                            Uuid.random().toString()
                        }
                        with(sharedTransitionScope) {
                            ComicCard(
                                comic = comic,
                                coverModifier = Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(
                                        ComicSharedElementKey(
                                            transitionId = transitionId,
                                            element = ComicSharedElement.Cover,
                                        )
                                    ),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                ),
                                titleModifier = Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(
                                        ComicSharedElementKey(
                                            transitionId = transitionId,
                                            element = ComicSharedElement.Title,
                                        )
                                    ),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                ),
                                onItemClick = { onComicClick(comic, transitionId) },
                            )
                        }
                    }
                }

                when (comics.loadState.append) {
                    is LoadState.Loading -> item(
                        key = "feed-append-loading",
                        contentType = "append-state",
                    ) {
                        Text(
                            text = stringResource(R.string.feed_loading_more),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .testTag(FeedTestTags.AppendLoading),
                        )
                    }

                    is LoadState.Error -> item(
                        key = "feed-append-error",
                        contentType = "append-state",
                    ) {
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .testTag(FeedTestTags.AppendError),
                        ) {
                            Text(stringResource(R.string.footer_error))
                        }
                    }

                    is LoadState.NotLoading -> {
                        if (comics.loadState.append.endOfPaginationReached) {
                            item(key = "feed-end", contentType = "append-state") {
                                Text(
                                    text = stringResource(R.string.feed_no_more),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                )
                            }
                        }
                    }
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
private fun FilterHeader(
    chips: List<FilterChipState>,
    onFilterChanged: (FeedFilterChange) -> Unit,

    summary: String,
    currentSortOrder: SortOrder,
    excludeTopicsGlobal: Boolean,
    onExcludeTopicsGlobalChanged: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = chips.sumOf { it.selected.size }

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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

            Text(
                text = stringResource(R.string.feed_current_sort, currentSortOrder.label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            if (selectedCount > 0) {

                TextButton(onClick = onClearFilters) {
                    Text(stringResource(R.string.feed_clear_filters))
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(
                items = chips,
                key = { it.group.label },
            ) { chipState ->
                FilterChip(
                    state = chipState,
                    onSelectionChanged = { option ->
                        onFilterChanged(chipState.changeFor(option))
                    },

                    excludeTopicsGlobal = excludeTopicsGlobal,
                    onExcludeTopicsGlobalChanged = onExcludeTopicsGlobalChanged,
                )
            }

            if (selectedCount > 0) {
                item(key = "feed-active-filter-count") {
                    Text(
                        text = stringResource(R.string.feed_active_filters, selectedCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun feedSummary(
    state: FeedUiState,
    itemCount: Int,
    refreshLoadState: LoadState,
): String = when {
    refreshLoadState is LoadState.Loading -> stringResource(R.string.feed_refreshing)
    refreshLoadState is LoadState.Error && itemCount == 0 -> stringResource(R.string.feed_load_failed)
    state.query.criteria.hasAnySelection ||
            state.data.globalPolicy.blockedTopicsEnabled ||
            state.data.globalPolicy.blockedTags.isNotEmpty() ->
        stringResource(R.string.feed_filtered_count, itemCount)


    else -> stringResource(R.string.feed_total_count, itemCount)
}

internal object FeedTestTags {
    const val Back = "feed-back"
    const val Refresh = "feed-refresh"
    const val List = "feed-list"
    const val InitialLoading = "feed-initial-loading"
    const val InitialError = "feed-initial-error"
    const val RefreshProgress = "feed-refresh-progress"
    const val RefreshError = "feed-refresh-error"
    const val Empty = "feed-empty"
    const val AppendLoading = "feed-append-loading"
    const val AppendError = "feed-append-error"
}
