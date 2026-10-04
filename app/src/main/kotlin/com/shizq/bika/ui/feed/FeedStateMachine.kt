@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.feed

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.domain.filter.FilterGroup
import com.shizq.bika.core.domain.filter.FilterOption
import com.shizq.bika.core.domain.filter.FilterSelections
import com.shizq.bika.core.domain.filter.toggle
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.preferences.ContentFilterPreferences
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first

/**
 * Feed 元数据状态机。
 *
 * 网络分页、刷新、追加加载及重试全部交给 Paging 3；这里仅管理排序、筛选与收藏偏好，
 * 避免同时存在两套分页状态源。
 */
class FeedStateMachine internal constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
) : FlowReduxStateMachineFactory<FeedUiState, FeedAction>() {

    init {
        initializeWith { FeedUiState.Initial() }

        spec {
            inState<FeedUiState.Initial> {
                onEnter {
                    val filter = userPreferencesDataSource.userData.first().filter
                    val data = snapshot.data.withPreferences(filter)
                    override { FeedUiState.Content(data = data) }
                }
            }

            inState<FeedUiState.Content> {
                collectWhileInState(userPreferencesDataSource.userData) { preferences ->
                    applyPreferences(preferences.filter)
                }

                on<FeedAction.ChangeSort> { action ->
                    mutateData {
                        copy(query = query.copy(sort = action.sort))
                    }
                }

                on<FeedAction.ClearFilters> {
                    if (snapshot.data.excludeTopicsGlobal) {
                        userPreferencesDataSource.setExcludeTopicsGlobal(false)
                    }
                    mutateData {
                        copy(
                            query = query.copy(localFilters = emptyMap()),
                            filterSelections = emptyMap(),
                            excludeTopicsGlobal = false,
                            globalBlockedTopics = emptyList(),
                        )
                    }
                }

                on<FeedAction.ToggleFilter> { action ->
                    toggleFilter(action.group, action.option)
                }

                on<FeedAction.SetGlobalTopicFilter> { action ->
                    setGlobalTopicFilter(action.enabled)
                }

                onActionEffect<FeedAction.AddFavorite> { action ->
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        addFavoriteTagToList(tags, action.tag)
                    }
                }
                onActionEffect<FeedAction.RemoveFavorite> { action ->
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        removeFavoriteTagFromList(tags, action.tag)
                    }
                }
                onActionEffect<FeedAction.RenameFavorite> { action ->
                    val name = normalizeFavoriteTagName(action.name) ?: return@onActionEffect
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        renameFavoriteTagInList(tags, action.tag, name)
                    }
                }
                onActionEffect<FeedAction.MoveFavorite> { action ->
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        moveFavoriteTagInList(tags, action.fromIndex, action.toIndex)
                    }
                }
                onActionEffect<FeedAction.AddCustomFavorite> { action ->
                    val name = normalizeFavoriteTagName(action.name) ?: return@onActionEffect
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        addFavoriteTagToList(
                            tags,
                            FavoriteTag(
                                name = name,
                                actionType = FeedActionType.AdvancedSearch.storageValue,
                            ),
                        )
                    }
                }
            }
        }
    }

    private suspend fun ChangeableState<FeedUiState.Content>.applyPreferences(
        filter: ContentFilterPreferences,
    ): ChangedState<FeedUiState> = mutateData { withPreferences(filter) }

    private suspend fun ChangeableState<FeedUiState.Content>.toggleFilter(
        group: FilterGroup,
        option: FilterOption,
    ): ChangedState<FeedUiState> {
        if (group is FilterGroup.ExcludeTopic && snapshot.data.excludeTopicsGlobal) {
            val topic = (option as? FilterOption.Topic)?.name ?: return noChange()
            userPreferencesDataSource.toggleGlobalExcludedTopic(topic)
            val topics = if (topic in snapshot.data.globalBlockedTopics) {
                snapshot.data.globalBlockedTopics - topic
            } else {
                snapshot.data.globalBlockedTopics + topic
            }
            return mutateData {
                copy(
                    globalBlockedTopics = topics,
                    filterSelections = query.localFilters.withGlobalTopics(true, topics),
                )
            }
        }

        return mutateData {
            val localFilters = query.localFilters.toggle(group, option)
            copy(
                query = query.copy(localFilters = localFilters),
                filterSelections = localFilters.withGlobalTopics(
                    enabled = excludeTopicsGlobal,
                    topics = globalBlockedTopics,
                ),
            )
        }
    }

    private suspend fun ChangeableState<FeedUiState.Content>.setGlobalTopicFilter(
        enabled: Boolean,
    ): ChangedState<FeedUiState> {
        if (enabled == snapshot.data.excludeTopicsGlobal) return noChange()

        return if (enabled) {
            val topics = snapshot.data.query.localFilters[FilterGroup.ExcludeTopic]
                .orEmpty()
                .filterIsInstance<FilterOption.Topic>()
                .map(FilterOption.Topic::name)
            userPreferencesDataSource.enableGlobalTopicBlock(topics)
            mutateData {
                val localFilters = query.localFilters - FilterGroup.ExcludeTopic
                copy(
                    query = query.copy(localFilters = localFilters),
                    filterSelections = localFilters.withGlobalTopics(true, topics),
                    excludeTopicsGlobal = true,
                    globalBlockedTopics = topics,
                )
            }
        } else {
            val topics = snapshot.data.globalBlockedTopics
            userPreferencesDataSource.setExcludeTopicsGlobal(false)
            mutateData {
                val localFilters = if (topics.isEmpty()) {
                    query.localFilters - FilterGroup.ExcludeTopic
                } else {
                    query.localFilters + (FilterGroup.ExcludeTopic to topics.map(FilterOption::Topic))
                }
                copy(
                    query = query.copy(localFilters = localFilters),
                    filterSelections = localFilters,
                    excludeTopicsGlobal = false,
                )
            }
        }
    }

    private fun ChangeableState<FeedUiState.Content>.mutateData(
        transform: FeedData.() -> FeedData,
    ): ChangedState<FeedUiState> = mutate {
        val nextData = data.transform()
        copy(
            data = nextData,
        )
    }
}

class FeedStateMachineFactory @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
) {
    fun create(): FeedStateMachine = FeedStateMachine(
        userPreferencesDataSource = userPreferencesDataSource,
    )
}

private fun FeedData.withPreferences(filter: ContentFilterPreferences): FeedData = copy(
    filterSelections = query.localFilters.withGlobalTopics(
        enabled = filter.globalTopicBlockEnabled,
        topics = filter.globalBlockedTopics,
    ),
    favoriteTags = filter.favoriteTags,
    excludeTopicsGlobal = filter.globalTopicBlockEnabled,
    globalBlockedTopics = filter.globalBlockedTopics,
    blockedTags = filter.blockedTags,
)

private fun FilterSelections.withGlobalTopics(
    enabled: Boolean,
    topics: List<String>,
): FilterSelections = if (!enabled) {
    this
} else if (topics.isEmpty()) {
    this - FilterGroup.ExcludeTopic
} else {
    this + (FilterGroup.ExcludeTopic to topics.map(FilterOption::Topic))
}
