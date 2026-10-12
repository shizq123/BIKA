package com.shizq.bika.ui.feed

import com.shizq.bika.core.domain.filter.FeedCriteria
import com.shizq.bika.core.domain.filter.FilterOption
import com.shizq.bika.core.domain.filter.FilterSelections
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.navigation.DiscoveryAction

data class FeedQuery(
    val sort: SortOrder = SortOrder.NEWEST,
    val criteria: FeedCriteria = FeedCriteria(),
)

/** Feed 页面只保存源状态，最终过滤策略在 ViewModel 边界编译。 */
data class FeedData(
    val query: FeedQuery = FeedQuery(),
    val favoriteTags: List<FavoriteTag> = emptyList(),
    val excludeTopicsGlobal: Boolean = false,
    val globalBlockedTopics: Set<String> = emptySet(),
    val blockedTags: Set<String> = emptySet(),
) {
    /** 旧 FilterChip 的 UI 投影；只展示本页筛选，全局规则由独立开关表达。 */
    val filterSelections: FilterSelections
        get() = query.criteria.toSelections()
}

data class FeedUiState(
    val data: FeedData = FeedData(),
    val preferencesReady: Boolean = false,
) {
    val query: FeedQuery get() = data.query
    val filterSelections: FilterSelections get() = data.filterSelections
    val favoriteTags: List<FavoriteTag> get() = data.favoriteTags
    val excludeTopicsGlobal: Boolean get() = data.excludeTopicsGlobal
}

sealed interface FeedAction {
    data object ClearFilters : FeedAction
    data class ChangeSort(val sort: SortOrder) : FeedAction
    data class ToggleIncludedTopic(val topic: String) : FeedAction
    data class ToggleExcludedTopic(val topic: String) : FeedAction
    data class ToggleFinishedState(val finished: Boolean) : FeedAction
    data class ToggleEpisodeRange(val range: FilterOption.CountRange) : FeedAction
    data class TogglePageRange(val range: FilterOption.CountRange) : FeedAction

    data class SetGlobalTopicFilter(val enabled: Boolean) : FeedAction
    data class AddFavorite(val tag: FavoriteTag) : FeedAction
    data class RemoveFavorite(val tag: FavoriteTag) : FeedAction
    data class RenameFavorite(val tag: FavoriteTag, val name: String) : FeedAction
    data class MoveFavorite(val fromIndex: Int, val toIndex: Int) : FeedAction
    data class AddCustomFavorite(val name: String) : FeedAction
}

sealed interface FeedDestination {
    data object Back : FeedDestination
    data class Comic(
        val id: String,
        val coverUrl: String,
        val title: String,
        val transitionId: String,
    ) : FeedDestination
    data class Feed(val action: DiscoveryAction) : FeedDestination
    data object BlockedTags : FeedDestination
    data object AddFavorite : FeedDestination
    data class RenameFavorite(val tag: FavoriteTag) : FeedDestination
}


