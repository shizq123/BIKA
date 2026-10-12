package com.shizq.bika.ui.feed

import com.shizq.bika.core.domain.filter.FilterGroup
import com.shizq.bika.core.domain.filter.FilterOption
import com.shizq.bika.core.domain.filter.FilterSelections
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.navigation.DiscoveryAction

data class FeedQuery(
    val sort: SortOrder = SortOrder.NEWEST,
    /** 仅保存当前页面的选择；全局屏蔽主题不再搬运到这里。 */
    val localFilters: FilterSelections = emptyMap(),
)

/**
 * Feed 页面会话状态。
 *
 * filterSelections 不再作为字段保存，它是 localFilters 与全局策略的派生结果。
 */
data class FeedData(
    val query: FeedQuery = FeedQuery(),
    val favoriteTags: List<FavoriteTag> = emptyList(),
    val excludeTopicsGlobal: Boolean = false,
    val globalBlockedTopics: Set<String> = emptySet(),
    val blockedTags: Set<String> = emptySet(),
) {
    val filterSelections: FilterSelections
        get() = query.localFilters.withGlobalTopics(
            enabled = excludeTopicsGlobal,
            topics = globalBlockedTopics,
        )

    val hasActiveFilters: Boolean
        get() = filterSelections.values.any { it.isNotEmpty() } || blockedTags.isNotEmpty()
}

sealed interface FeedUiState {
    val data: FeedData

    data class Initial(
        override val data: FeedData = FeedData(),
    ) : FeedUiState

    data class Content(
        override val data: FeedData,
    ) : FeedUiState
}

val FeedUiState.query: FeedQuery get() = data.query
val FeedUiState.filterSelections: FilterSelections get() = data.filterSelections
val FeedUiState.favoriteTags: List<FavoriteTag> get() = data.favoriteTags
val FeedUiState.excludeTopicsGlobal: Boolean get() = data.excludeTopicsGlobal

sealed interface FeedAction {
    /** 只清空当前页面的筛选，不关闭全局屏蔽策略。 */
    data object ClearFilters : FeedAction
    data class ChangeSort(val sort: SortOrder) : FeedAction
    data class ToggleFilter(
        val group: FilterGroup,
        val option: FilterOption,
    ) : FeedAction

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

private fun FilterSelections.withGlobalTopics(
    enabled: Boolean,
    topics: Set<String>,
): FilterSelections {
    if (!enabled || topics.isEmpty()) return this

    val localTopics = this[FilterGroup.ExcludeTopic]
        .orEmpty()
        .filterIsInstance<FilterOption.Topic>()
        .map(FilterOption.Topic::name)
        .toSet()

    return this + (
            FilterGroup.ExcludeTopic to
                    (localTopics + topics).map(FilterOption::Topic)
            )
}

