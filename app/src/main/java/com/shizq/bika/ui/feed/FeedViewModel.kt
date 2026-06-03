package com.shizq.bika.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.DetailedHistory
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
import com.shizq.bika.util.injectLocalStatusFrom
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
    private val historyDao: ReadingHistoryDao,
    @Assisted private val action: DiscoveryAction,
) : ViewModel() {
    val currentSortOrder: StateFlow<Sort>
        field = MutableStateFlow(Sort.NEWEST)
    val filterSelections: StateFlow<Map<FilterGroup, List<String>>>
        field = MutableStateFlow(emptyMap())
    val currentPage: StateFlow<Int>
        field = MutableStateFlow(1)
    val totalPages: StateFlow<Int>
        field = MutableStateFlow(1)

    // 网络数据（含 PagingSource 首次加载时的一次性本地状态注入）
    private val basePagedComics: Flow<PagingData<ComicSimple>> = combine(
        currentSortOrder,
        currentPage
    ) { sort, page ->
        sort to page
    }.flatMapLatest { (sort, page) ->
        Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 0,
                enablePlaceholders = false
            ),
            initialKey = page
        ) {
            createPagingSource(action, sort)
        }.flow
    }.cachedIn(viewModelScope)

    // combine DB Flow：每当收藏/进度变化时，对当前 PagingData 快照重新注入本地状态
    // 这样无需 invalidate/reload 整个列表，只需 map 重算徽章字段即可。
    private val statusEnrichedComics: Flow<PagingData<ComicSimple>> = combine(
        basePagedComics,
        historyDao.getDetailedHistories(),
    ) { pagingData, histories ->
        pagingData.map { comic -> comic.injectSingleFrom(histories) }
    }

    val pagedComics: Flow<PagingData<ComicSimple>> = combine(
        statusEnrichedComics,
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
        currentPage.value = 1
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
        currentPage.value = 1
        currentSortOrder.update { newSort }
    }

    fun updatePage(page: Int) {
        val target = page.coerceIn(1, totalPages.value)
        currentPage.value = target
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

                is FilterGroup.ExcludeTopic -> {
                    // 排除勾选的主题分类：漫画所有的 categories 里必须没有任何一个被包含在 selectedValues 中
                    selectedValues.none { it in comic.categories }
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

                is FilterGroup.EpsRange -> {
                    // 话数范围：OR 逻辑，满足任意一个区间即可
                    selectedValues.any { label ->
                        matchesEpsRange(comic.epsCount, label)
                    }
                }

                is FilterGroup.PagesRange -> {
                    // 页数范围：OR 逻辑，满足任意一个区间即可
                    selectedValues.any { label ->
                        matchesPagesRange(comic.pagesCount, label)
                    }
                }
            }
            // AND 逻辑：只要有任意一组条件不满足，这本漫画就被过滤掉
            if (!matchesGroup) {
                return false
            }
        }

        // 所有组的条件都通过了，保留这本漫画
        return true
    }

    private fun matchesEpsRange(epsCount: Int, label: String): Boolean = when (label) {
        "单话 (1话)" -> epsCount == 1
        "短篇 (2-5话)" -> epsCount in 2..5
        "中篇 (6-20话)" -> epsCount in 6..20
        "长篇 (21-100话)" -> epsCount in 21..100
        "超长篇 (100话以上)" -> epsCount > 100
        else -> true
    }

    private fun matchesPagesRange(pagesCount: Int, label: String): Boolean = when (label) {
        "少页 (<50页)" -> pagesCount in 1..<50
        "中等 (50-200页)" -> pagesCount in 50..200
        "多页 (200-500页)" -> pagesCount in 201..500
        "超多页 (500页以上)" -> pagesCount > 500
        else -> true
    }


    private fun createPagingSource(
        action: DiscoveryAction,
        sort: Sort
    ): PagingSource<Int, ComicSimple> {
        val source = when (action) {
            is DiscoveryAction.Channel -> channelPagingSourceFactory.create(action.name, sort)
            is DiscoveryAction.Knight -> advancedSearchPagingSourceFactory.create(action.name, sort)
            is DiscoveryAction.AdvancedSearch -> advancedSearchPagingSourceFactory.create(
                action.name,
                sort
            )

            is DiscoveryAction.ToFavourite -> favouriteComicsPagingSourceFactory.create(sort)

            DiscoveryAction.ToCollections -> SinglePagePagingSource {
                totalPages.value = 1
                api.getCollections().collections.firstOrNull()?.comics ?: emptyList()
            }

            DiscoveryAction.ToRandom -> SinglePagePagingSource {
                totalPages.value = 1
                api.getRandomComics().comics
            }

            DiscoveryAction.ToRecent -> recentUpdatesPagingSourceProvider.get()
        }

        when (source) {
            is ChannelPagingSource -> source.onPageInfoLoaded = { pages, _ ->
                totalPages.value = pages
            }
            is AdvancedSearchPagingSource -> source.onPageInfoLoaded = { pages, _ ->
                totalPages.value = pages
            }
            is RecentUpdatesPagingSource -> source.onPageInfoLoaded = { pages, _ ->
                totalPages.value = pages
            }
            is FavouriteComicsPagingSource -> source.onPageInfoLoaded = { pages, _ ->
                totalPages.value = pages
            }
            else -> {
                totalPages.value = 1
            }
        }

        return source
    }

    @AssistedFactory
    interface Factory {
        fun create(
            action: DiscoveryAction,
        ): FeedViewModel
    }
}

/**
 * 对单个 ComicSimple 从 DetailedHistory 列表快照中注入本地状态。
 * 供 PagingData.map{} 使用，此处不能是挂起函数。
 */
private fun ComicSimple.injectSingleFrom(histories: List<DetailedHistory>): ComicSimple {
    val detailed = histories.find { it.history.id == id } ?: return this
    val lastProgress = detailed.progressList.maxByOrNull { it.lastReadAt }
    val progressText = if (lastProgress != null) {
        val epsCount = detailed.history.epsCount
        when {
            epsCount > lastProgress.chapterId -> "有更新"
            lastProgress.chapterId >= epsCount
                    && lastProgress.currentPage >= lastProgress.pageCount
                    && lastProgress.pageCount > 0 -> "已读完"
            else -> "已阅读"
        }
    } else null
    return copy(
        isFavourited = detailed.history.isFavourited,
        lastReadChapterProgress = progressText
    )
}