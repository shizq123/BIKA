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
import com.shizq.bika.paging.FavouriteComicsPagingSource
import com.shizq.bika.paging.SinglePagePagingSource
import com.shizq.bika.paging.TopicComicsPagingSource
import com.shizq.bika.ui.comiclist.ComicSearchParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = FeedViewModel.Factory::class)
class FeedViewModel @AssistedInject constructor(
    private val api: BikaDataSource,
    topicComicsPagingSourceFactory: TopicComicsPagingSource.Factory,
    favouriteComicsPagingSourceFactory: FavouriteComicsPagingSource.Factory,
    @Assisted private val action: DiscoveryAction,
) : ViewModel() {
    private val pagingSource: PagingSource<Int, ComicSimple> = when (action) {
        is DiscoveryAction.Knight -> topicComicsPagingSourceFactory.create(
            ComicSearchParams(
                knightId = action.id,
                sort = Sort.NEWEST
            )
        )

        is DiscoveryAction.AdvancedSearch -> topicComicsPagingSourceFactory.create(
            ComicSearchParams(
                topic = action.topic,
                tag = action.tag,
                authorName = action.authorName,
                translationTeam = action.translationTeam,
                sort = Sort.NEWEST
            )
        )

        DiscoveryAction.ToCollections -> SinglePagePagingSource {
            val collectionsData = api.getCollections()
            collectionsData.collections.firstOrNull()?.comics ?: emptyList()
        }

        DiscoveryAction.ToRandom -> SinglePagePagingSource {
            val collectionsData = api.getRandomComics()
            collectionsData.comics
        }

        DiscoveryAction.ToRecent -> topicComicsPagingSourceFactory.create(
            ComicSearchParams(sort = Sort.NEWEST)
        )

        DiscoveryAction.ToFavourite -> favouriteComicsPagingSourceFactory.create(Sort.NEWEST.value)
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