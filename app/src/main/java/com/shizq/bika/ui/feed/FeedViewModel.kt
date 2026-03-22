package com.shizq.bika.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
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
import javax.inject.Provider

@HiltViewModel(assistedFactory = FeedViewModel.Factory::class)
class FeedViewModel @AssistedInject constructor(
    private val api: BikaDataSource,
    channelPagingSourceFactory: ChannelPagingSource.Factory,
    favouriteComicsPagingSourceFactory: FavouriteComicsPagingSource.Factory,
    advancedSearchPagingSourceFactory: AdvancedSearchPagingSource.Factory,
    recentUpdatesPagingSourceProvider: Provider<RecentUpdatesPagingSource>,
    @Assisted private val action: DiscoveryAction,
) : ViewModel() {
    private val pagingSource: PagingSource<Int, ComicSimple> = when (action) {
        is DiscoveryAction.Channel->channelPagingSourceFactory.create(action.name, Sort.NEWEST)
        is DiscoveryAction.Knight -> advancedSearchPagingSourceFactory.create(
            query = action.name,
            sort = Sort.NEWEST
        )

        is DiscoveryAction.AdvancedSearch -> advancedSearchPagingSourceFactory.create(
            query = action.name,
            sort = Sort.NEWEST
        )

        DiscoveryAction.ToCollections -> SinglePagePagingSource {
            val collectionsData = api.getCollections()
            collectionsData.collections.firstOrNull()?.comics ?: emptyList()
        }

        DiscoveryAction.ToRandom -> SinglePagePagingSource {
            val collectionsData = api.getRandomComics()
            collectionsData.comics
        }

        DiscoveryAction.ToRecent -> recentUpdatesPagingSourceProvider.get()

        DiscoveryAction.ToFavourite -> favouriteComicsPagingSourceFactory.create(Sort.NEWEST)
    }
    val pagedComics = Pager(PagingConfig(40)) {
        pagingSource
    }.flow
        .cachedIn(viewModelScope)

    @AssistedFactory
    interface Factory {
        fun create(
            action: DiscoveryAction,
        ): FeedViewModel
    }
}