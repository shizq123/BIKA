package com.shizq.bika.ui.comiclist

import androidx.compose.ui.util.fastAny
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.shizq.bika.core.data.repository.TagsRepository
import com.shizq.bika.core.model.ComicSimple
import com.shizq.bika.core.model.Sort
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.ui.tag.FilterGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopicViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val api: BikaDataSource,
    private val topicComicsPagingSourceFactory: TopicComicsPagingSource.Factory,
    private val tagsRepository: TagsRepository,
) : ViewModel() {
    private val topicType = savedStateHandle.getStateFlow(SEARCH_TYPE, TopicType.Latest.key)
    val topicText = savedStateHandle.getStateFlow(SEARCH_TITLE, "")
    private val topic = savedStateHandle.getStateFlow(SEARCH_QUERY, "")
    private val sortOrder = savedStateHandle.getStateFlow(SORT_ORDER, Sort.NEWEST)
    private val selectedFilters = MutableStateFlow<Map<FilterGroup, List<String>>>(emptyMap())

    val searchParametersFlow =
        combine(topicType, topic, sortOrder, selectedFilters) { typeKey, query, sort, filterMap ->
            SearchParameters(
                type = TopicType.fromKey(typeKey),
                query = query,
                sort = sort,
                filters = filterMap
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchParameters(null, "", Sort.NEWEST, emptyMap())
        )

    val availableTags: StateFlow<List<String>> = tagsRepository.getTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pagedComics: Flow<PagingData<ComicSimple>> = searchParametersFlow
        .flatMapLatest { params ->
            createComicsPagerFlow(params)
                .map { pagingData ->
                    pagingData.map { comic ->
                        viewModelScope.launch {
                            tagsRepository.saveTags(comic.tags)
                        }
                        comic
                    }
                }
                .map { pagingData ->
                    val topicFilters = params.filters[FilterGroup.Topic] ?: emptyList()
                    val statusFilters = params.filters[FilterGroup.Status] ?: emptyList()

                    if (topicFilters.isEmpty() && statusFilters.isEmpty()) {
                        return@map pagingData
                    }

                    pagingData.filter { comic ->
                        val topicMatch = if (topicFilters.isEmpty()) {
                            true
                        } else {
                            comic.categories.fastAny { it in topicFilters }
                        }

                        val statusMatch = when {
                            statusFilters.isEmpty() || statusFilters.size > 1 -> true
                            "完结" in statusFilters -> comic.finished
                            else -> true
                        }

                        topicMatch && statusMatch
                    }
                }
        }
        .cachedIn(viewModelScope)

    fun onSortOrderChanged(sort: Sort) {
        savedStateHandle[SORT_ORDER] = sort
    }

    fun updateFilters(newFilters: Map<FilterGroup, List<String>>) {
        selectedFilters.update { newFilters }
    }

    private fun createComicsPagerFlow(
        params: SearchParameters,
    ): Flow<PagingData<ComicSimple>> {
        if (params.type == null) {
            return flowOf(PagingData.empty())
        }

        val apiParams = when (params.type) {
            is TopicType.Categories -> ComicSearchParams(topic = params.query, sort = params.sort)
            is TopicType.Latest -> ComicSearchParams(sort = params.sort)
            is TopicType.Tags -> ComicSearchParams(tag = params.query, sort = params.sort)
            is TopicType.Author -> ComicSearchParams(authorName = params.query, sort = params.sort)
            is TopicType.Knight -> ComicSearchParams(knightId = params.query, sort = params.sort)
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

data class SearchParameters(
    val type: TopicType?,
    val query: String,
    val sort: Sort,
    val filters: Map<FilterGroup, List<String>>
)

private const val SEARCH_TYPE = "tag"
private const val SEARCH_TITLE = "title"
private const val SEARCH_QUERY = "value"
private const val SORT_ORDER = "sort_order"
private val PAGING_CONFIG = PagingConfig(pageSize = 40)