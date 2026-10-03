package com.shizq.bika.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.domain.filter.FilterGroup
import com.shizq.bika.core.domain.filter.FilterOption
import com.shizq.bika.core.domain.filter.FilterSelections
import com.shizq.bika.core.domain.filter.hasAnySelection
import com.shizq.bika.core.domain.filter.matchesFilters
import com.shizq.bika.core.domain.filter.toggle
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.navigation.DiscoveryAction
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = FeedViewModel.Factory::class)
class FeedViewModel @AssistedInject constructor(
    private val api: BikaDataSource,
    private val historyDao: ReadingHistoryDao,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    @Assisted private val action: DiscoveryAction,
) : ViewModel() {
    private val query = MutableStateFlow(FeedQuery())
    private val reloadSignal = MutableStateFlow(0)
    private var latestLoadRequest: FeedLoadRequest? = null

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    val currentAction: DiscoveryAction = action

    init {
        viewModelScope.launch {
            historyDao.getDetailedHistories().collect { histories ->
                _uiState.update { it.copy(detailedHistories = histories) }
            }
        }

        viewModelScope.launch {
            userPreferencesDataSource.userData.collect { preferences ->
                _uiState.update {
                    it.copy(
                        favoriteTags = preferences.filter.favoriteTags,
                        excludeTopicsGlobal = preferences.filter.globalTopicBlockEnabled,
                    )
                }
            }
        }

        viewModelScope.launch {
            combine(
                query,
                userPreferencesDataSource.userData,
                reloadSignal,
            ) { currentQuery, preferences, reloadVersion ->
                val effectiveFilters = currentQuery.localFilters.withGlobalTopics(
                    enabled = preferences.filter.globalTopicBlockEnabled,
                    topics = preferences.filter.globalBlockedTopics,
                )
                FeedLoadRequest(
                    query = currentQuery,
                    effectiveFilters = effectiveFilters,
                    blockedTags = preferences.filter.blockedTags,
                    reloadVersion = reloadVersion,
                )
            }
                .distinctUntilChanged()
                .collectLatest(::load)
        }
    }

    fun dispatch(action: FeedAction) {
        when (action) {
            FeedAction.Retry -> reloadSignal.update { it + 1 }
            is FeedAction.ChangePage -> changePage(action.page)
            is FeedAction.ChangeSort -> query.update {
                it.copy(page = 1, sort = action.sort)
            }

            is FeedAction.ToggleFilter -> toggleFilter(action.group, action.option)
            is FeedAction.SetGlobalTopicFilter -> setGlobalTopicFilter(action.enabled)
            is FeedAction.AddFavorite -> addFavoriteTag(action.tag)
            is FeedAction.RemoveFavorite -> removeFavoriteTag(action.tag)
            is FeedAction.RenameFavorite -> updateFavoriteTagName(action.tag, action.name)
            is FeedAction.MoveFavorite -> moveFavoriteTag(action.fromIndex, action.toIndex)
            is FeedAction.AddCustomFavorite -> addCustomFavoriteTag(action.name)
        }
    }

    private fun changePage(page: Int) {
        val totalPages = (_uiState.value.content as? FeedContentState.Success)
            ?.page
            ?.totalPages
            ?: 1
        query.update { it.copy(page = page.coerceIn(1, totalPages.coerceAtLeast(1))) }
    }

    private fun toggleFilter(group: FilterGroup, option: FilterOption) {
        if (group is FilterGroup.ExcludeTopic && _uiState.value.excludeTopicsGlobal) {
            val topic = (option as? FilterOption.Topic)?.name ?: return
            query.update { it.copy(page = 1) }
            viewModelScope.launch {
                userPreferencesDataSource.toggleGlobalExcludedTopic(topic)
            }
            return
        }

        query.update {
            it.copy(
                page = 1,
                localFilters = it.localFilters.toggle(group, option),
            )
        }
    }

    private fun setGlobalTopicFilter(enabled: Boolean) {
        query.update { it.copy(page = 1) }
        viewModelScope.launch {
            if (enabled) {
                val localExcluded = query.value.localFilters[FilterGroup.ExcludeTopic]
                    .orEmpty()
                    .filterIsInstance<FilterOption.Topic>()
                    .map { it.name }
                userPreferencesDataSource.enableGlobalTopicBlock(localExcluded)
            } else {
                userPreferencesDataSource.setExcludeTopicsGlobal(false)
                val globalExcluded =
                    userPreferencesDataSource.userData.first().filter.globalBlockedTopics
                query.update { current ->
                    current.copy(
                        page = 1,
                        localFilters = if (globalExcluded.isEmpty()) {
                            current.localFilters - FilterGroup.ExcludeTopic
                        } else {
                            current.localFilters + (
                                    FilterGroup.ExcludeTopic to
                                            globalExcluded.map(FilterOption::Topic)
                                    )
                        },
                    )
                }
            }
        }
    }

    private suspend fun load(request: FeedLoadRequest) {
        latestLoadRequest = request
        val previousContent = _uiState.value.content
        _uiState.update {
            it.copy(
                query = request.query,
                filterSelections = request.effectiveFilters,
                content = when (previousContent) {
                    is FeedContentState.Success -> previousContent.copy(
                        refreshState = FeedRefreshState.Loading(request.query),
                    )

                    FeedContentState.Initial,
                    FeedContentState.Loading,
                    is FeedContentState.Error -> FeedContentState.Loading
                },
            )
        }

        try {
            val rawPage = loadPage(request.query)
            val visibleItems = rawPage.items
                .filter { comic ->
                    !request.effectiveFilters.hasAnySelection ||
                            matchesFilters(comic, request.effectiveFilters)
                }
                .filter { comic ->
                    request.blockedTags.isEmpty() ||
                            comic.tags.none { it in request.blockedTags }
                }

            _uiState.update { current ->
                if (latestLoadRequest != request) {
                    current
                } else {
                    current.copy(
                        content = FeedContentState.Success(
                            page = rawPage.copy(items = visibleItems),
                            displayedQuery = request.query,
                            isClientFiltered = request.effectiveFilters.hasAnySelection ||
                                    request.blockedTags.isNotEmpty(),
                        ),
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { current ->
                if (latestLoadRequest != request) {
                    current
                } else {
                    current.copy(
                        content = when (val content = current.content) {
                            is FeedContentState.Success -> content.copy(
                                refreshState = FeedRefreshState.Failed(
                                    targetQuery = request.query,
                                    error = FeedError.LoadFailed,
                                ),
                            )

                            FeedContentState.Initial,
                            FeedContentState.Loading,
                            is FeedContentState.Error -> FeedContentState.Error(FeedError.LoadFailed)
                        },
                    )
                }
            }
        }
    }

    private suspend fun loadPage(query: FeedQuery): FeedPage = when (val target = action) {
        is DiscoveryAction.Channel -> api.searchComics(
            topic = target.name,
            sort = query.sort,
            page = query.page,
        ).comics.toFeedPage(query.page)

        is DiscoveryAction.Knight -> api.searchComics(
            knightId = target.id,
            sort = query.sort,
            page = query.page,
        ).comics.toFeedPage(query.page)

        is DiscoveryAction.AdvancedSearch -> api.advancedSearch(
            content = target.name,
            categories = emptyList(),
            sort = query.sort,
            page = query.page,
        ).comics.toFeedPage(query.page)

        DiscoveryAction.ToFavourite -> api.getFavouriteComics(
            sort = query.sort,
            page = query.page,
        ).comics.toFeedPage(query.page)

        DiscoveryAction.ToRecent -> api.searchComics(
            sort = SortOrder.NEWEST,
            page = query.page,
        ).comics.toFeedPage(query.page)

        DiscoveryAction.ToCollections -> {
            val items = api.getCollections().collections.firstOrNull()?.comics.orEmpty()
            FeedPage(items = items, page = 1, totalPages = 1, totalCount = items.size)
        }

        DiscoveryAction.ToRandom -> {
            val items = api.getRandomComics().comics
            FeedPage(items = items, page = 1, totalPages = 1, totalCount = items.size)
        }
    }

    private fun addFavoriteTag(tag: FavoriteTag) {
        viewModelScope.launch {
            userPreferencesDataSource.updateFavoriteTags { tags ->
                addFavoriteTagToList(tags, tag)
            }
        }
    }

    private fun removeFavoriteTag(tag: FavoriteTag) {
        viewModelScope.launch {
            userPreferencesDataSource.updateFavoriteTags { tags ->
                removeFavoriteTagFromList(tags, tag)
            }
        }
    }

    private fun updateFavoriteTagName(tag: FavoriteTag, newName: String) {
        val normalizedName = normalizeFavoriteTagName(newName) ?: return
        viewModelScope.launch {
            userPreferencesDataSource.updateFavoriteTags { tags ->
                renameFavoriteTagInList(tags, tag, normalizedName)
            }
        }
    }

    private fun moveFavoriteTag(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            userPreferencesDataSource.updateFavoriteTags { tags ->
                moveFavoriteTagInList(tags, fromIndex, toIndex)
            }
        }
    }

    private fun addCustomFavoriteTag(name: String) {
        val normalizedName = normalizeFavoriteTagName(name) ?: return
        addFavoriteTag(
            FavoriteTag(
                name = normalizedName,
                actionType = FeedActionType.AdvancedSearch.storageValue,
            )
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(action: DiscoveryAction): FeedViewModel
    }
}

private fun com.shizq.bika.core.network.model.PageData<ComicSummary>.toFeedPage(
    requestedPage: Int,
): FeedPage = FeedPage(
    items = docs,
    page = requestedPage.coerceAtLeast(1),
    totalPages = pages.coerceAtLeast(1),
    totalCount = total,
)

private fun FilterSelections.withGlobalTopics(
    enabled: Boolean,
    topics: List<String>,
): FilterSelections {
    if (!enabled) return this
    return if (topics.isEmpty()) {
        this - FilterGroup.ExcludeTopic
    } else {
        this + (FilterGroup.ExcludeTopic to topics.map(FilterOption::Topic))
    }
}


private data class FeedLoadRequest(
    val query: FeedQuery,
    val effectiveFilters: FilterSelections,
    val blockedTags: Set<String>,
    val reloadVersion: Int,
)


