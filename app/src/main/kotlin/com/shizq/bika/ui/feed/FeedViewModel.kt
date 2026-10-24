@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.paging.filter
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.domain.filter.ComicFilterPolicy
import com.shizq.bika.core.domain.filter.GlobalContentPolicy
import com.shizq.bika.core.domain.filter.compileFilterPolicy
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.paging.AdvancedSearchPagingSource
import com.shizq.bika.paging.ChannelPagingSource
import com.shizq.bika.paging.FavouriteComicsPagingSource
import com.shizq.bika.paging.FixedPagePagingSource
import com.shizq.bika.paging.KnightPagingSource
import com.shizq.bika.paging.RecentUpdatesPagingSource
import com.shizq.bika.paging.SinglePagePagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Feed 生命周期适配层；Paging 3 是列表数据与加载状态的唯一数据源。 */
@HiltViewModel(assistedFactory = FeedViewModel.Factory::class)
class FeedViewModel @AssistedInject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val api: BikaDataSource,
    private val channelPagingSourceFactory: ChannelPagingSource.Factory,
    private val knightPagingSourceFactory: KnightPagingSource.Factory,
    private val advancedSearchPagingSourceFactory: AdvancedSearchPagingSource.Factory,
    private val favouriteComicsPagingSourceFactory: FavouriteComicsPagingSource.Factory,
    private val recentUpdatesPagingSourceProvider: jakarta.inject.Provider<RecentUpdatesPagingSource>,
    @Assisted val currentAction: DiscoveryAction,
) : ViewModel() {

    private val query = MutableStateFlow(FeedQuery())
    private val supportsPagination = currentAction != DiscoveryAction.ToCollections &&
        currentAction != DiscoveryAction.ToRandom
    private val pagingController = FeedPagingController(supportsPagination)

    init {
        viewModelScope.launch {
            combine(
                query.map { it.sort }.distinctUntilChanged(),
                userPreferencesDataSource.userData
                    .map { it.app.continuousScrollEnabled }
                    .distinctUntilChanged(),
            ) { sort, continuous -> sort to continuous }
                .collect { (sort, continuous) -> pagingController.configure(sort, continuous) }
        }
    }

    val uiState = combine(
        query,
        userPreferencesDataSource.userData,
        pagingController.state,
    ) { query, preferences, paging ->
        val filter = preferences.filter
        FeedUiState(
            data = FeedData(
                query = query,
                favoriteTags = filter.favoriteTags,
                globalPolicy = GlobalContentPolicy(
                    blockedTopicsEnabled = filter.globalTopicBlockEnabled,
                    blockedTopics = filter.globalBlockedTopics.toSet(),
                    blockedTags = filter.blockedTags,
                ),
            ),
            preferencesReady = paging.request?.let {
                it.sort == query.sort && it.continuousScrollEnabled ==
                    (preferences.app.continuousScrollEnabled || !supportsPagination)
            } == true,
            pagination = FeedPagination(
                continuousScrollEnabled = paging.request?.continuousScrollEnabled
                    ?: preferences.app.continuousScrollEnabled,
                currentPage = paging.request?.page ?: 1,
                totalPages = paging.totalPages,
                supportsPagination = supportsPagination,
                revision = paging.request?.revision ?: 0,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState(),
    )

    private val filterPolicy: Flow<ComicFilterPolicy> = uiState
        .filter { it.preferencesReady }
        .map { state ->
            compileFilterPolicy(
                local = state.data.query.criteria,
                global = state.data.globalPolicy,
            )
        }
        .distinctUntilChanged()

    private var pagingScope: CoroutineScope? = null

    internal val pagingSession = pagingController.state
        .map { it.request }
        .filterNotNull()
        .distinctUntilChanged()
        .mapLatest { request ->
            pagingScope?.cancel()
            val scope = CoroutineScope(
                viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]),
            ).also { pagingScope = it }
            val remotePages = Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    initialLoadSize = PAGE_SIZE,
                    prefetchDistance = 4,
                    enablePlaceholders = false,
                ),
                pagingSourceFactory = {
                    val source = createPagingSource(request.sort)
                    val selected = if (request.continuousScrollEnabled) {
                        source
                    } else {
                        FixedPagePagingSource(source, request.page)
                    }
                    pagingController.track(request, selected)
                },
            ).flow.cachedIn(scope)
            FeedPagingSession(
                request = request,
                comics = combine(remotePages, filterPolicy) { pagingData, policy ->
                    if (policy.isEmpty) pagingData else pagingData.filter(policy::matches)
                }.cachedIn(scope),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun createPagingSource(sort: SortOrder): PagingSource<Int, ComicSummary> =
        when (val action = currentAction) {
            is DiscoveryAction.Channel -> channelPagingSourceFactory.create(action.name, sort)
            is DiscoveryAction.Knight -> knightPagingSourceFactory.create(action.id, sort)
            is DiscoveryAction.AdvancedSearch -> advancedSearchPagingSourceFactory.create(action.name, sort)
            DiscoveryAction.ToFavourite -> favouriteComicsPagingSourceFactory.create(sort)
            DiscoveryAction.ToRecent -> recentUpdatesPagingSourceProvider.get()
            DiscoveryAction.ToCollections -> SinglePagePagingSource {
                api.getCollections().collections.firstOrNull()?.comics.orEmpty()
            }
            DiscoveryAction.ToRandom -> SinglePagePagingSource {
                api.getRandomComics().comics
            }
        }

    fun dispatch(action: FeedAction) {
        when (action) {
            FeedAction.ClearFilters -> query.update {
                it.copy(criteria = it.criteria.clear())
            }

            is FeedAction.ChangeSort -> query.update { it.copy(sort = action.sort) }

            FeedAction.PreviousPage -> pagingController.state.value.request?.let {
                pagingController.changePage(it.page - 1)
            }

            FeedAction.NextPage -> pagingController.state.value.request?.let {
                if (it.page < Int.MAX_VALUE) pagingController.changePage(it.page + 1)
            }

            is FeedAction.JumpToPage -> pagingController.changePage(action.page)

            is FeedAction.ToggleIncludedTopic -> query.update {
                it.copy(criteria = it.criteria.toggleIncludedTopic(action.topic))
            }

            is FeedAction.ToggleExcludedTopic -> query.update {
                it.copy(criteria = it.criteria.toggleExcludedTopic(action.topic))
            }

            is FeedAction.ToggleStatus -> query.update {
                it.copy(criteria = it.criteria.toggleStatus(action.status))
            }

            is FeedAction.ToggleEpisodeRange -> query.update {
                it.copy(criteria = it.criteria.toggleEpisodeRange(action.range))
            }

            is FeedAction.TogglePageRange -> query.update {
                it.copy(criteria = it.criteria.togglePageRange(action.range))
            }

            is FeedAction.SetGlobalTopicFilter -> persist {
                userPreferencesDataSource.setExcludeTopicsGlobal(action.enabled)
            }

            is FeedAction.AddFavorite -> persist {
                userPreferencesDataSource.updateFavoriteTags { tags ->
                    addFavoriteTagToList(tags, action.tag)
                }
            }

            is FeedAction.RemoveFavorite -> persist {
                userPreferencesDataSource.updateFavoriteTags { tags ->
                    removeFavoriteTagFromList(tags, action.tag)
                }
            }

            is FeedAction.RenameFavorite -> {
                val name = normalizeFavoriteTagName(action.name) ?: return
                persist {
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        renameFavoriteTagInList(tags, action.tag, name)
                    }
                }
            }

            is FeedAction.MoveFavorite -> persist {
                userPreferencesDataSource.updateFavoriteTags { tags ->
                    moveFavoriteTagInList(tags, action.fromIndex, action.toIndex)
                }
            }

            is FeedAction.AddCustomFavorite -> {
                val name = normalizeFavoriteTagName(action.name) ?: return
                persist {
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        addFavoriteTagToList(
                            tags,
                            FavoriteTag(
                                name = name,
                                actionType = FeedActionType.AdvancedSearch.storageValue,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    @AssistedFactory
    interface Factory {
        fun create(action: DiscoveryAction): FeedViewModel
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
