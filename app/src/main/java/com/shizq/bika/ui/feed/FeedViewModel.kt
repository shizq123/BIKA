package com.shizq.bika.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import com.shizq.bika.core.model.ComicSimple
import com.shizq.bika.core.model.Sort
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.paging.AdvancedSearchPagingSource
import com.shizq.bika.paging.ChannelPagingSource
import com.shizq.bika.paging.FavouriteComicsPagingSource
import com.shizq.bika.paging.RecentUpdatesPagingSource
import com.shizq.bika.paging.SinglePagePagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import javax.inject.Provider

@HiltViewModel(assistedFactory = FeedViewModel.Factory::class)
class FeedViewModel @AssistedInject constructor(
    private val api: BikaDataSource,
    private val channelPagingSourceFactory: ChannelPagingSource.Factory,
    private val favouriteComicsPagingSourceFactory: FavouriteComicsPagingSource.Factory,
    private val advancedSearchPagingSourceFactory: AdvancedSearchPagingSource.Factory,
    private val recentUpdatesPagingSourceProvider: Provider<RecentUpdatesPagingSource>,
    @Assisted private val action: DiscoveryAction,
) : ViewModel() {
    val currentSortOrder: StateFlow<Sort>
        field = MutableStateFlow(Sort.NEWEST)
    val pagedComics : Flow<PagingData<ComicSimple>> = currentSortOrder
        .flatMapLatest { sort ->
            Pager(PagingConfig(pageSize = 40)) {
                createPagingSource(action, sort)
            }.flow
        }
        .cachedIn(viewModelScope)

    fun updateSortOrder(newSort: Sort) {
        currentSortOrder.update { newSort }
    }

    private fun createPagingSource(
        action: DiscoveryAction,
        sort: Sort
    ): PagingSource<Int, ComicSimple> {
        return when (action) {
            is DiscoveryAction.Channel -> channelPagingSourceFactory.create(action.name, sort)
            is DiscoveryAction.Knight -> advancedSearchPagingSourceFactory.create(action.name, sort)
            is DiscoveryAction.AdvancedSearch -> advancedSearchPagingSourceFactory.create(
                action.name,
                sort
            )

            is DiscoveryAction.ToFavourite -> favouriteComicsPagingSourceFactory.create(sort)

            DiscoveryAction.ToCollections -> SinglePagePagingSource {
                api.getCollections().collections.firstOrNull()?.comics ?: emptyList()
            }

            DiscoveryAction.ToRandom -> SinglePagePagingSource {
                api.getRandomComics().comics
            }

            DiscoveryAction.ToRecent -> recentUpdatesPagingSourceProvider.get()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            action: DiscoveryAction,
        ): FeedViewModel
    }
}