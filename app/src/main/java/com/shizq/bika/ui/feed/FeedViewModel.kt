package com.shizq.bika.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.paging.filter as pagingFilter
import androidx.paging.map as pagingMap
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
import com.shizq.bika.util.computeProgressText
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
import kotlinx.coroutines.flow.map
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

    // 合并 sort、page、filter、DB history 变化，构建统一的分页数据流。
    // 注意：PagingData 不能在 cachedIn 之后再次被 combine/map，否则会运行时崩溃。
    // 因此将所有 map/filter 操作放在 cachedIn 之前。
    val pagedComics: Flow<PagingData<ComicSimple>> = combine(
        currentSortOrder,
        currentPage,
        filterSelections
    ) { sort, page, filters ->
        Triple(sort, page, filters)
    }.flatMapLatest { (sort, page, filters) ->
        Pager(
            config = PagingConfig(
                pageSize = 40
            ),
            initialKey = page
        ) {
            createPagingSource(action, sort)
        }.flow.map { pd ->
            if (filters.isEmpty() || filters.values.all { it.isEmpty() }) {
                pd
            } else {
                pd.pagingFilter { comic -> matchesFilters(comic, filters) }
            }
        }
    }.cachedIn(viewModelScope)

    val detailedHistories = historyDao.getDetailedHistories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

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

    // matchesFilters、matchesEpsRange、matchesPagesRange 已提取为文件顶层私有函数


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
 * 为避免重复逻辑，调用 ComicStatusInjector.kt 中的 injectLocalStatusFrom。
 */
fun ComicSimple.injectSingleFrom(histories: List<DetailedHistory>): ComicSimple {
    val historyMap = histories.associateBy { it.history.id }
    val detailed = historyMap[id] ?: return this
    val lastProgress = detailed.progressList.maxByOrNull { it.lastReadAt }
    val progressText = computeProgressText(lastProgress, detailed.history.epsCount)
    return copy(
        isFavourited = detailed.history.isFavourited,
        lastReadChapterProgress = progressText
    )
}


private fun matchesFilters(
    comic: ComicSimple,
    filters: Map<FilterGroup, List<String>>
): Boolean {
    for ((group, selectedValues) in filters) {
        if (selectedValues.isEmpty()) continue
        val matchesGroup = when (group) {
            is FilterGroup.Topic -> selectedValues.any { it in comic.categories }
            is FilterGroup.ExcludeTopic -> selectedValues.none { it in comic.categories }
            is FilterGroup.Status -> {
                val isFinishedSelected = "完结" in selectedValues
                val isOngoingSelected = "连载" in selectedValues
                when {
                    isFinishedSelected && isOngoingSelected -> true
                    isFinishedSelected -> comic.finished
                    isOngoingSelected -> !comic.finished
                    else -> true
                }
            }
            is FilterGroup.EpsRange -> selectedValues.any { matchesEpsRange(comic.epsCount, it) }
            is FilterGroup.PagesRange -> selectedValues.any { matchesPagesRange(comic.pagesCount, it) }
        }
        if (!matchesGroup) return false
    }
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

private fun matchesPagesRange(pagesCount: Int, label: String): Boolean {
    if (pagesCount <= 0) return true // 列表接口未返回页数时，默认为 0，不进行过滤，避免列表为空
    if (label.startsWith("指定数量: ")) {
        val text = label.substringAfter("指定数量: ").replace("页", "").trim()
        if (text.contains("-")) {
            val parts = text.split("-")
            if (parts.size == 2) {
                val start = parts[0].trim().toIntOrNull()
                val end = parts[1].trim().toIntOrNull()
                if (start != null && end != null) {
                    return pagesCount in start..end
                }
            }
        } else if (text.startsWith(">=")) {
            val target = text.substring(2).trim().toIntOrNull()
            if (target != null) return pagesCount >= target
        } else if (text.startsWith("<=")) {
            val target = text.substring(2).trim().toIntOrNull()
            if (target != null) return pagesCount <= target
        } else if (text.startsWith(">")) {
            val target = text.substring(1).trim().toIntOrNull()
            if (target != null) return pagesCount > target
        } else if (text.startsWith("<")) {
            val target = text.substring(1).trim().toIntOrNull()
            if (target != null) return pagesCount < target
        } else {
            val target = text.toIntOrNull()
            if (target != null) return pagesCount == target
        }
        return false
    }
    return when (label) {
        "少页 (<50页)" -> pagesCount in 1..<50
        "中等 (50-200页)" -> pagesCount in 50..200
        "多页 (200-500页)" -> pagesCount in 201..500
        "超多页 (500页以上)" -> pagesCount > 500
        else -> true
    }
}