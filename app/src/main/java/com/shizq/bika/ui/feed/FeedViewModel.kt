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
import com.shizq.bika.navigation.FeedNavKey
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
    @Assisted private val feedNavKey: FeedNavKey,
) : ViewModel() {
    private val pagingSource: PagingSource<Int, ComicSimple> = when (feedNavKey) {
        FeedNavKey.Collection -> {
            SinglePagePagingSource {
                val collectionsData = api.getCollections()
                collectionsData.collections.firstOrNull()?.comics ?: emptyList()
            }
        }

        FeedNavKey.Random -> SinglePagePagingSource {
            val collectionsData = api.getRandomComics()
            collectionsData.comics
        }

        is FeedNavKey.Topic -> {
            topicComicsPagingSourceFactory.create(
                ComicSearchParams(topic = feedNavKey.name, sort = Sort.NEWEST)
            )
        }

        is FeedNavKey.Recent -> {
            topicComicsPagingSourceFactory.create(
                ComicSearchParams(sort = Sort.NEWEST)
            )
        }

        is FeedNavKey.Knight -> topicComicsPagingSourceFactory.create(
            ComicSearchParams(knightId = feedNavKey.id)
        )
    }
    val pagedComics = Pager(PagingConfig(40, 1)) {
        pagingSource
    }.flow
        .cachedIn(viewModelScope)

    @AssistedFactory
    interface Factory {
        fun create(
            feedNavKey: FeedNavKey,
        ): FeedViewModel
    }
}