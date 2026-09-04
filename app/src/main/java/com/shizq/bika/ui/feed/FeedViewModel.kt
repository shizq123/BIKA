@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.domain.filter.FilterGroup
import com.shizq.bika.domain.filter.FilterOption
import com.shizq.bika.domain.filter.FilterSelections
import com.shizq.bika.domain.filter.hasAnySelection
import com.shizq.bika.domain.filter.matchesFilters
import com.shizq.bika.domain.filter.toggle
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.paging.AdvancedSearchPagingSource
import com.shizq.bika.paging.ChannelPagingSource
import com.shizq.bika.paging.FavouriteComicsPagingSource
import com.shizq.bika.paging.RecentUpdatesPagingSource
import com.shizq.bika.paging.SinglePagePagingSource
import com.shizq.bika.util.computeProgressText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Provider
import androidx.paging.filter as pagingFilter

@HiltViewModel(assistedFactory = FeedViewModel.Factory::class)
class FeedViewModel @AssistedInject constructor(
    private val api: BikaDataSource,
    private val channelPagingSourceFactory: ChannelPagingSource.Factory,
    private val favouriteComicsPagingSourceFactory: FavouriteComicsPagingSource.Factory,
    private val advancedSearchPagingSourceFactory: AdvancedSearchPagingSource.Factory,
    private val recentUpdatesPagingSourceProvider: Provider<RecentUpdatesPagingSource>,
    private val historyDao: ReadingHistoryDao,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    @Assisted private val action: DiscoveryAction,
) : ViewModel() {
    val currentSortOrder: StateFlow<SortOrder>
        field = MutableStateFlow(SortOrder.NEWEST)

    private val localFilterSelections = MutableStateFlow<FilterSelections>(emptyMap())

    val filterSelections: StateFlow<FilterSelections> = combine(
        localFilterSelections,
        userPreferencesDataSource.userData
    ) { local, prefs ->
        if (!prefs.filter.globalTopicBlockEnabled) return@combine local
        val globalTopics = prefs.filter.globalBlockedTopics.map(FilterOption::Topic)
        // 全局主题为空时移除该键，而不是写入空列表，避免下游出现"键存在但值为空"
        if (globalTopics.isEmpty()) {
            local - FilterGroup.ExcludeTopic
        } else {
            local + (FilterGroup.ExcludeTopic to globalTopics)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap()
    )

    val excludeTopicsGlobal: StateFlow<Boolean> = userPreferencesDataSource.userData
        .map { it.filter.globalTopicBlockEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    val currentPage: StateFlow<Int>
        field = MutableStateFlow(1)
    val totalPages: StateFlow<Int>
        field = MutableStateFlow(1)

    // 合并 sort、page、filter、blockedTags 变化，构建统一的分页数据流。
    // 注意：PagingData 不能在 cachedIn 之后再次被 combine/map，否则会运行时崩溃。
    // 因此将所有 map/filter 操作放在 cachedIn 之前。
    val pagedComics: Flow<PagingData<ComicSummary>> = combine(
        currentSortOrder,
        currentPage,
        filterSelections,
        userPreferencesDataSource.userData.map { it.filter.blockedTags }.distinctUntilChanged()
    ) { sort, page, filters, blockedTags ->
        BlockedFilterState(sort, page, filters, blockedTags)
    }.flatMapLatest { state ->
        Pager(
            config = PagingConfig(
                pageSize = 40
            ),
            initialKey = state.page
        ) {
            createPagingSource(action, state.sort)
        }.flow.map { pd ->
            val step1 = if (state.filters.hasAnySelection) {
                pd.pagingFilter { comic -> matchesFilters(comic, state.filters) }
            } else {
                pd
            }
            if (state.blockedTags.isEmpty()) {
                step1
            } else {
                step1.pagingFilter { comic -> comic.tags.none { it in state.blockedTags } }
            }
        }
    }.cachedIn(viewModelScope)

    val detailedHistories = historyDao.getDetailedHistories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun toggleFilter(group: FilterGroup, option: FilterOption) {
        // 排除主题处于全局模式时，选中态的唯一来源是 DataStore，不写本地状态
        if (group is FilterGroup.ExcludeTopic && excludeTopicsGlobal.value) {
            val topic = (option as? FilterOption.Topic)?.name ?: return
            viewModelScope.launch {
                userPreferencesDataSource.toggleGlobalExcludedTopic(topic)
            }
            return
        }

        currentPage.value = 1
        localFilterSelections.update { it.toggle(group, option) }
    }

    fun toggleExcludeTopicsGlobal(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                // 开启与写入初始主题必须是同一次写入，否则会短暂出现「已开启但主题为旧值」的状态，
                // 触发一次多余的 Pager 重建。
                val localExcluded = localFilterSelections.value[FilterGroup.ExcludeTopic]
                    .orEmpty()
                    .filterIsInstance<FilterOption.Topic>()
                    .map { it.name }
                userPreferencesDataSource.enableGlobalTopicBlock(localExcluded)
            } else {
                userPreferencesDataSource.setExcludeTopicsGlobal(false)
                val globalExcluded =
                    userPreferencesDataSource.userData.first().filter.globalBlockedTopics
                // 关闭全局后，把原全局主题落回本地状态，避免用户看到筛选被清空
                localFilterSelections.update { current ->
                    if (globalExcluded.isEmpty()) {
                        current - FilterGroup.ExcludeTopic
                    } else {
                        current + (FilterGroup.ExcludeTopic to globalExcluded.map(FilterOption::Topic))
                    }
                }
            }
        }
    }

    fun updateSortOrder(newSort: SortOrder) {
        currentPage.value = 1
        currentSortOrder.update { newSort }
    }

    fun updatePage(page: Int) {
        val target = page.coerceIn(1, totalPages.value.coerceAtLeast(1))
        currentPage.value = target
    }

    val currentAction: DiscoveryAction = action

    val favoriteTags: StateFlow<List<FavoriteTag>> = userPreferencesDataSource.userData
        .map { it.filter.favoriteTags }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addFavoriteTag(tag: FavoriteTag) {
        viewModelScope.launch {
            userPreferencesDataSource.updateFavoriteTags { tags ->
                if (tags.any { it.isSameTag(tag) }) tags else tags + tag
            }
        }
    }

    fun removeFavoriteTag(tag: FavoriteTag) {
        viewModelScope.launch {
            userPreferencesDataSource.updateFavoriteTags { tags ->
                tags.filterNot { it.isSameTag(tag) }
            }
        }
    }

    fun updateFavoriteTagName(tag: FavoriteTag, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            userPreferencesDataSource.updateFavoriteTags { tags ->
                tags.map { if (it.isSameTag(tag)) it.copy(name = newName) else it }
            }
        }
    }

    fun moveFavoriteTag(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            userPreferencesDataSource.updateFavoriteTags { tags ->
                if (fromIndex in tags.indices && toIndex in tags.indices) {
                    tags.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                } else {
                    tags
                }
            }
        }
    }

    fun addCustomFavoriteTag(name: String) {
        if (name.isBlank()) return
        addFavoriteTag(FavoriteTag(name = name, actionType = "AdvancedSearch"))
    }

    private fun createPagingSource(
        action: DiscoveryAction,
        sort: SortOrder
    ): PagingSource<Int, ComicSummary> {
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
 * 对单个 ComicSummary 从预先构建的 id -> DetailedHistory 映射中注入本地状态。
 * 映射由调用方（UI 层）在列表外构建一次，避免每个列表项重复 O(N) 扫描。
 * 逻辑复用 ComicStatusInjector.kt 中的 injectLocalStatusFrom。
 */
fun ComicSummary.injectFromHistoryMap(historyMap: Map<String, DetailedHistory>): ComicSummary {
    val detailed = historyMap[id] ?: return this
    val lastProgress = detailed.progressList.maxByOrNull { it.lastReadAt }
    val progressText = computeProgressText(lastProgress, detailed.history.epsCount)
    return copy(
        isFavourited = detailed.history.isFavourited,
        lastReadChapterProgress = progressText
    )
}


/** 收藏标签的身份判定：name + actionType 构成业务主键。 */
private fun FavoriteTag.isSameTag(other: FavoriteTag): Boolean =
    name == other.name && actionType == other.actionType

fun DiscoveryAction.toFavoriteTag(): FavoriteTag? {
    return when (this) {
        is DiscoveryAction.Channel -> FavoriteTag(name = name, actionType = "Channel")
        is DiscoveryAction.Knight -> FavoriteTag(name = name, actionType = "Knight", actionId = id)
        is DiscoveryAction.AdvancedSearch -> FavoriteTag(name = name, actionType = "AdvancedSearch")
        else -> null
    }
}

fun FavoriteTag.toAction(): DiscoveryAction {
    return when (actionType) {
        "Channel" -> DiscoveryAction.Channel(name)
        "Knight" -> DiscoveryAction.Knight(name, actionId)
        else -> DiscoveryAction.AdvancedSearch(name)
    }
}

private data class BlockedFilterState(
    val sort: SortOrder,
    val page: Int,
    val filters: FilterSelections,
    val blockedTags: Set<String>
)