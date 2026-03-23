package com.shizq.bika.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.paging.filter
import com.shizq.bika.core.model.ComicSimple
import com.shizq.bika.core.model.Sort
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.paging.AdvancedSearchPagingSource
import com.shizq.bika.paging.ChannelPagingSource
import com.shizq.bika.paging.FavouriteComicsPagingSource
import com.shizq.bika.paging.RecentUpdatesPagingSource
import com.shizq.bika.paging.SinglePagePagingSource
import com.shizq.bika.ui.tag.FilterGroup
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val filterSelections: StateFlow<Map<FilterGroup, List<String>>>
        field = MutableStateFlow(emptyMap())

    private val basePagedComics: Flow<PagingData<ComicSimple>> = currentSortOrder
        .flatMapLatest { sort ->
            Pager(PagingConfig(pageSize = 40)) {
                createPagingSource(action, sort)
            }.flow
        }
        .cachedIn(viewModelScope)

    val pagedComics: Flow<PagingData<ComicSimple>> = combine(
        basePagedComics,
        filterSelections
    ) { pagingData, filters ->
        if (filters.isEmpty() || filters.values.all { it.isEmpty() }) {
            pagingData
        } else {
            pagingData.filter { comic ->
                matchesFilters(comic, filters)
            }
        }
    }

    fun toggleFilter(group: FilterGroup, value: String) {
        filterSelections.update { currentMap ->
            val newMap = currentMap.toMutableMap()
            val currentList = newMap[group]?.toMutableList() ?: mutableListOf()

            if (currentList.contains(value)) {
                currentList.remove(value)
            } else {
                currentList.add(value)
            }
            if (currentList.isEmpty()) {
                newMap.remove(group)
            } else {
                newMap[group] = currentList
            }

            newMap
        }
    }

    fun updateSortOrder(newSort: Sort) {
        currentSortOrder.update { newSort }
    }
    private fun matchesFilters(
        comic: ComicSimple,
        filters: Map<FilterGroup, List<String>>
    ): Boolean {
        for ((group, selectedValues) in filters) {
            if (selectedValues.isEmpty()) continue
            // 检查当前这本漫画是否符合这一组的条件
            val matchesGroup = when (group) {
                is FilterGroup.Topic -> {
                    selectedValues.any { it in comic.categories }
                }

                is FilterGroup.Status -> {
                    val isFinishedSelected = "完结" in selectedValues
                    val isOngoingSelected = "连载" in selectedValues
                    if (isFinishedSelected && isOngoingSelected) {
                        true // 两个都选了，等于没限制
                    } else if (isFinishedSelected) {
                        comic.finished // 必须是 true
                    } else if (isOngoingSelected) {
                        !comic.finished // finished 必须是 false
                    } else {
                        true
                    }
                }
                // 兜底逻辑
                else -> true
            }
            // AND 逻辑：只要有任意一组条件不满足，这本漫画就被过滤掉
            if (!matchesGroup) {
                return false
            }
        }

        // 所有组的条件都通过了，保留这本漫画
        return true
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