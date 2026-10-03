package com.shizq.bika.ui.feed

import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.domain.filter.FilterGroup
import com.shizq.bika.core.domain.filter.FilterOption
import com.shizq.bika.core.domain.filter.FilterSelections
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.navigation.DiscoveryAction

data class FeedQuery(
    val page: Int = 1,
    val sort: SortOrder = SortOrder.NEWEST,
    val localFilters: FilterSelections = emptyMap(),
)

data class FeedPage(
    val items: List<ComicSummary>,
    val page: Int,
    val totalPages: Int,
    val totalCount: Int?,
)

data class FeedUiState(
    val query: FeedQuery = FeedQuery(),
    val filterSelections: FilterSelections = emptyMap(),
    val content: FeedContentState = FeedContentState.Initial,
    val detailedHistories: List<DetailedHistory> = emptyList(),
    val favoriteTags: List<FavoriteTag> = emptyList(),
    val excludeTopicsGlobal: Boolean = false,
    val globalBlockedTopics: List<String> = emptyList(),
    val blockedTags: Set<String> = emptySet(),
    val preferencesReady: Boolean = false,
)

sealed interface FeedRefreshState {
    data object Idle : FeedRefreshState
    data class Loading(val targetQuery: FeedQuery) : FeedRefreshState
    data class Failed(
        val targetQuery: FeedQuery,
        val error: FeedError,
    ) : FeedRefreshState
}

sealed interface FeedContentState {
    data object Initial : FeedContentState
    data object Loading : FeedContentState
    data class Error(val reason: FeedError) : FeedContentState
    data class Success(
        val page: FeedPage,
        val displayedQuery: FeedQuery = FeedQuery(page = page.page),
        val isClientFiltered: Boolean = false,
        val refreshState: FeedRefreshState = FeedRefreshState.Idle,
    ) : FeedContentState
}

sealed interface FeedAction {
    data object Retry : FeedAction
    data class ChangePage(val page: Int) : FeedAction
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
    data class Comic(val id: String) : FeedDestination
    data class Feed(val action: DiscoveryAction) : FeedDestination
    data object BlockedTags : FeedDestination
    data class PageJump(val currentPage: Int, val totalPages: Int) : FeedDestination
    data object AddFavorite : FeedDestination
    data class RenameFavorite(val tag: FavoriteTag) : FeedDestination
}

sealed interface FeedError {
    data object LoadFailed : FeedError
}
