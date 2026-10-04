package com.shizq.bika.ui.feed

import com.shizq.bika.core.domain.filter.FilterGroup
import com.shizq.bika.core.domain.filter.FilterOption
import com.shizq.bika.core.domain.filter.FilterSelections
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.navigation.DiscoveryAction

data class FeedQuery(
    val sort: SortOrder = SortOrder.NEWEST,
    val localFilters: FilterSelections = emptyMap(),
)

/** Feed 的非分页数据；列表内容与加载状态由 Paging 3 单独管理。 */
data class FeedData(
    val query: FeedQuery = FeedQuery(),
    val filterSelections: FilterSelections = emptyMap(),
    val favoriteTags: List<FavoriteTag> = emptyList(),
    val excludeTopicsGlobal: Boolean = false,
    val globalBlockedTopics: List<String> = emptyList(),
    val blockedTags: Set<String> = emptySet(),
)

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
