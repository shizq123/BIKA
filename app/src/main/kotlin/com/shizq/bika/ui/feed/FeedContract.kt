package com.shizq.bika.ui.feed

import com.shizq.bika.core.domain.filter.ComicStatus
import com.shizq.bika.core.domain.filter.FeedCriteria
import com.shizq.bika.core.domain.filter.GlobalContentPolicy
import com.shizq.bika.core.domain.filter.IntRangeSpec
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
    val globalPolicy: GlobalContentPolicy = GlobalContentPolicy(),
)

data class FeedUiState(
    val data: FeedData = FeedData(),
    val preferencesReady: Boolean = false,
) {
    val query: FeedQuery get() = data.query
    val favoriteTags: List<FavoriteTag> get() = data.favoriteTags
    val excludeTopicsGlobal: Boolean get() = data.globalPolicy.blockedTopicsEnabled
}

sealed interface FeedAction {
    data object ClearFilters : FeedAction
    data class ChangeSort(val sort: SortOrder) : FeedAction
    data class ToggleIncludedTopic(val topic: String) : FeedAction
    data class ToggleExcludedTopic(val topic: String) : FeedAction
    data class ToggleStatus(val status: ComicStatus) : FeedAction
    data class ToggleEpisodeRange(val range: IntRangeSpec) : FeedAction
    data class TogglePageRange(val range: IntRangeSpec) : FeedAction

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


