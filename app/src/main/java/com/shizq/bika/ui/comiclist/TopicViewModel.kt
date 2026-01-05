package com.shizq.bika.ui.comiclist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.ComicSimple
import com.shizq.bika.core.network.model.Sort
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@HiltViewModel
class TopicViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val api: BikaDataSource,
    private val topicComicsPagingSourceFactory: TopicComicsPagingSource.Factory,
) : ViewModel() {
    private val topicType = savedStateHandle.getStateFlow(SEARCH_TYPE, TopicType.Latest.key)
    val topic = savedStateHandle.getStateFlow(SEARCH_QUERY, "")
    private val sortOrder = savedStateHandle.getStateFlow(SORT_ORDER, Sort.NEWEST)
    private val searchParametersFlow =
        combine(topicType, topic, sortOrder) { typeKey, query, sort ->
            SearchParameters(
                type = TopicType.fromKey(typeKey),
                query = query,
                sort = sort
            )
        }
    val pagedComics = searchParametersFlow
        .flatMapLatest { params ->
            createComicsPagerFlow(params)
        }
        .cachedIn(viewModelScope)

    fun onSortOrderChanged(sort: Sort) {
        savedStateHandle[SORT_ORDER] = sort
    }

    private fun createComicsPagerFlow(params: SearchParameters): Flow<PagingData<ComicSimple>> {
        if (params.type == null) {
            return flowOf(PagingData.empty())
        }

        val apiParams = when (params.type) {
            is TopicType.Categories -> ComicSearchParams(topic = params.query, sort = params.sort)
            is TopicType.Latest -> ComicSearchParams(sort = params.sort)
            is TopicType.Tags -> ComicSearchParams(tag = params.query, sort = params.sort)
            is TopicType.Author -> ComicSearchParams(authorName = params.query, sort = params.sort)
            is TopicType.Knight -> ComicSearchParams(knight = params.query, sort = params.sort)
            is TopicType.Translate -> ComicSearchParams(
                translationTeam = params.query,
                sort = params.sort
            )
        }

        return Pager(
            config = PAGING_CONFIG,
        ) {
            topicComicsPagingSourceFactory(apiParams)
        }.flow
    }
}

private data class SearchParameters(
    val type: TopicType?,
    val query: String,
    val sort: Sort
)

private const val SEARCH_TYPE = "tag"
private const val SEARCH_QUERY = "value"
private const val SORT_ORDER = "sort_order"
private val PAGING_CONFIG = PagingConfig(pageSize = 40)