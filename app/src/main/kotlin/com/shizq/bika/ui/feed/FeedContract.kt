package com.shizq.bika.ui.feed

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

/** 跨页面阶段保留的 Feed 数据。 */
data class FeedData(
    val query: FeedQuery = FeedQuery(),
    val filterSelections: FilterSelections = emptyMap(),
    val favoriteTags: List<FavoriteTag> = emptyList(),
    val excludeTopicsGlobal: Boolean = false,
    val globalBlockedTopics: List<String> = emptyList(),
    val blockedTags: Set<String> = emptySet(),
    val continuousScrollEnabled: Boolean = true,
)

/**
 * Feed 的互斥页面状态。
 *
 * [Initial] 不只是占位符：进入它会读取首次请求所需的偏好并加载页面。
 * 已经拥有可展示内容后，后续请求不会退回 Initial，而由 [Content.refreshState] 表示。
 */
sealed interface FeedUiState {
    val data: FeedData

    data class Initial(
        override val data: FeedData = FeedData(),
    ) : FeedUiState

    data class Content(
        override val data: FeedData,
        val page: FeedPage,
        val displayedQuery: FeedQuery,
        val isClientFiltered: Boolean = false,
        val refreshState: FeedRefreshState = FeedRefreshState.Idle,
    ) : FeedUiState

    data class Error(
        override val data: FeedData,
        val reason: FeedError,
    ) : FeedUiState
}

// 页面只读投影，避免 UI 为公共数据关心具体状态分支。
val FeedUiState.query: FeedQuery get() = data.query
val FeedUiState.filterSelections: FilterSelections get() = data.filterSelections
val FeedUiState.favoriteTags: List<FavoriteTag> get() = data.favoriteTags
val FeedUiState.excludeTopicsGlobal: Boolean get() = data.excludeTopicsGlobal
val FeedUiState.continuousScrollEnabled: Boolean get() = data.continuousScrollEnabled

sealed interface FeedRefreshState {
    data object Idle : FeedRefreshState
    data class Loading(val targetQuery: FeedQuery) : FeedRefreshState
    data object Appending : FeedRefreshState
    data class Failed(
        val targetQuery: FeedQuery,
        val error: FeedError,
    ) : FeedRefreshState
}

sealed interface FeedAction {
    data object Retry : FeedAction
    data class ChangePage(val page: Int) : FeedAction
    data object AppendNextPage : FeedAction
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
