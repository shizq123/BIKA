@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.shizq.bika.core.domain.filter.FilterSelections
import com.shizq.bika.core.domain.filter.hasAnySelection
import com.shizq.bika.core.domain.filter.matchesFilters
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.paging.AdvancedSearchPagingSource
import com.shizq.bika.paging.ChannelPagingSource
import com.shizq.bika.paging.FavouriteComicsPagingSource
import com.shizq.bika.paging.KnightPagingSource
import com.shizq.bika.paging.RecentUpdatesPagingSource
import com.shizq.bika.paging.SinglePagePagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Feed 生命周期适配层；Paging 3 是列表数据与加载状态的唯一数据源。 */
@HiltViewModel(assistedFactory = FeedViewModel.Factory::class)
class FeedViewModel @AssistedInject constructor(
    stateMachineFactory: FeedStateMachineFactory,
    private val api: BikaDataSource,
    private val channelPagingSourceFactory: ChannelPagingSource.Factory,
    private val knightPagingSourceFactory: KnightPagingSource.Factory,
    private val advancedSearchPagingSourceFactory: AdvancedSearchPagingSource.Factory,
    private val favouriteComicsPagingSourceFactory: FavouriteComicsPagingSource.Factory,
    private val recentUpdatesPagingSourceProvider: jakarta.inject.Provider<RecentUpdatesPagingSource>,
    @Assisted val currentAction: DiscoveryAction,
) : ViewModel() {

    private val stateMachine = stateMachineFactory
        .create()
        .launchIn(viewModelScope)

    val uiState = stateMachine.state

    val comics: Flow<PagingData<ComicSummary>> = uiState
        .filterIsInstance<FeedUiState.Content>()
        .map { it.data.toPagingRequest() }
        .distinctUntilChanged()
        .flatMapLatest { request ->
            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    initialLoadSize = PAGE_SIZE,
                    prefetchDistance = 4,
                    enablePlaceholders = false,
                ),
                pagingSourceFactory = {
                    when (val action = currentAction) {
                        is DiscoveryAction.Channel -> channelPagingSourceFactory.create(
                            channel = action.name,
                            sort = request.sort,
                        )

                        is DiscoveryAction.Knight -> knightPagingSourceFactory.create(
                            knightId = action.id,
                            sort = request.sort,
                        )

                        is DiscoveryAction.AdvancedSearch -> advancedSearchPagingSourceFactory.create(
                            query = action.name,
                            sort = request.sort,
                        )

                        DiscoveryAction.ToFavourite ->
                            favouriteComicsPagingSourceFactory.create(request.sort)

                        DiscoveryAction.ToRecent -> recentUpdatesPagingSourceProvider.get()

                        DiscoveryAction.ToCollections -> SinglePagePagingSource {
                            api.getCollections().collections.firstOrNull()?.comics.orEmpty()
                        }

                        DiscoveryAction.ToRandom -> SinglePagePagingSource {
                            api.getRandomComics().comics
                        }
                    }
                },
            ).flow.map { pagingData ->
                pagingData.filter { comic ->
                    (!request.filters.hasAnySelection || matchesFilters(comic, request.filters)) &&
                            (request.blockedTags.isEmpty() ||
                                    comic.tags.none(request.blockedTags::contains))
                }
            }
        }
        .cachedIn(viewModelScope)

    fun dispatch(action: FeedAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(action: DiscoveryAction): FeedViewModel
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}

private data class FeedPagingRequest(
    val sort: SortOrder,
    val filters: FilterSelections,
    val blockedTags: Set<String>,
)

private fun FeedData.toPagingRequest() = FeedPagingRequest(
    sort = query.sort,
    filters = filterSelections,
    blockedTags = blockedTags,
)
