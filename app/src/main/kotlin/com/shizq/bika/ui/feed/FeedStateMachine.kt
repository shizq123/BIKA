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
import com.shizq.bika.core.domain.filter.hasAnySelection
import com.shizq.bika.core.domain.filter.matchesFilters
import com.shizq.bika.core.domain.filter.toggle
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.core.model.preferences.ContentFilterPreferences
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.navigation.DiscoveryAction
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicLong

/**
 * Feed 的状态机。页面生命周期直接由 Initial / Content / Error 表达，不再嵌套第二层内容状态。
 */
class FeedStateMachine internal constructor(
    private val api: BikaDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val discoveryAction: DiscoveryAction,
) : FlowReduxStateMachineFactory<FeedUiState, FeedAction>() {

    private val loadGeneration = AtomicLong(0)
    @Volatile
    private var activeRequest: ActiveFeedRequest? = null

    init {
        initializeWith { FeedUiState.Initial() }

        spec {
            inState<FeedUiState.Initial> {
                onEnter {
                    val filter = userPreferencesDataSource.userData.first().filter
                    val data = snapshot.data.withPreferences(filter)
                    initialLoad(data)
                }
            }

            inState<FeedUiState.Error> {
                on<FeedAction.Retry> {
                    override { FeedUiState.Initial(data = snapshot.data) }
                }
            }

            inState<FeedUiState.Content> {
                collectWhileInState(userPreferencesDataSource.userData) { preferences ->
                    applyPreferences(preferences.filter)
                }

                on<FeedAction.Retry> {
                    refresh(snapshot.data.query, force = true)
                }

                on<FeedAction.ChangePage> { action ->
                    refresh(
                        snapshot.data.query.copy(
                            page = action.page.coerceIn(
                                1,
                                snapshot.page.totalPages.coerceAtLeast(1)
                            ),
                        ),
                    )
                }

                on<FeedAction.ChangeSort> { action ->
                    refresh(snapshot.data.query.copy(page = 1, sort = action.sort))
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

    private suspend fun ChangeableState<FeedUiState.Initial>.initialLoad(
        data: FeedData,
    ): ChangedState<FeedUiState> = try {
        val page = loadAndFilter(data.query, data.filterSelections, data.blockedTags)
        override {
            FeedUiState.Content(
                data = data,
                page = page,
                displayedQuery = data.query,
                isClientFiltered = data.isClientFiltered,
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        override { FeedUiState.Error(data = data, reason = FeedError.LoadFailed) }
    }

    private suspend fun ChangeableState<FeedUiState.Content>.applyPreferences(
        filter: ContentFilterPreferences,
    ): ChangedState<FeedUiState> {
        val query = snapshot.data.query
        val newData = snapshot.data.copy(query = query).withPreferences(filter)
        val affectsContent = newData.filterSelections != snapshot.data.filterSelections ||
                newData.blockedTags != snapshot.data.blockedTags

        return if (affectsContent) {
            val targetData = newData.copy(query = newData.query.copy(page = 1))
            val request = ActiveFeedRequest(
                query = targetData.query,
                filters = targetData.filterSelections,
                blockedTags = targetData.blockedTags,
            )
            if (activeRequest == request) {
                mutate { copy(data = targetData) }
            } else {
                refresh(
                    targetQuery = targetData.query,
                    baseData = targetData,
                )
            }
        } else {
            mutate { copy(data = newData) }
        }
    }

    private suspend fun ChangeableState<FeedUiState.Content>.toggleFilter(
        group: FilterGroup,
        option: FilterOption,
    ): ChangedState<FeedUiState> {
        if (group is FilterGroup.ExcludeTopic && snapshot.data.excludeTopicsGlobal) {
            val topic = (option as? FilterOption.Topic)?.name ?: return noChange()
            val topics = if (topic in snapshot.data.globalBlockedTopics) {
                snapshot.data.globalBlockedTopics - topic
            } else {
                snapshot.data.globalBlockedTopics + topic
            }
            userPreferencesDataSource.toggleGlobalExcludedTopic(topic)
            val nextData = snapshot.data.copy(
                query = snapshot.data.query.copy(page = 1),
                globalBlockedTopics = topics,
            )
            return refresh(
                targetQuery = nextData.query,
                baseData = nextData,
            )
        }

        val query = snapshot.data.query.copy(
            page = 1,
            localFilters = snapshot.data.query.localFilters.toggle(group, option),
        )
        return refresh(query)
    }

    private suspend fun ChangeableState<FeedUiState.Content>.setGlobalTopicFilter(
        enabled: Boolean,
    ): ChangedState<FeedUiState> {
        if (enabled == snapshot.data.excludeTopicsGlobal) return noChange()

        if (enabled) {
            val topics = snapshot.data.query.localFilters[FilterGroup.ExcludeTopic]
                .orEmpty()
                .filterIsInstance<FilterOption.Topic>()
                .map(FilterOption.Topic::name)
            val query = snapshot.data.query.copy(
                localFilters = snapshot.data.query.localFilters - FilterGroup.ExcludeTopic,
            )
            userPreferencesDataSource.enableGlobalTopicBlock(topics)
            return refresh(
                targetQuery = query,
                baseData = snapshot.data.copy(
                    query = query,
                    excludeTopicsGlobal = true,
                    globalBlockedTopics = topics,
                ),
            )
        } else {
            val topics = snapshot.data.globalBlockedTopics
            val query = snapshot.data.query.copy(
                localFilters = if (topics.isEmpty()) {
                    snapshot.data.query.localFilters - FilterGroup.ExcludeTopic
                } else {
                    snapshot.data.query.localFilters + (
                            FilterGroup.ExcludeTopic to topics.map(FilterOption::Topic)
                            )
                },
            )
            userPreferencesDataSource.setExcludeTopicsGlobal(false)
            return refresh(
                targetQuery = query,
                baseData = snapshot.data.copy(
                    query = query,
                    excludeTopicsGlobal = false,
                ),
            )
        }
    }

    private suspend fun ChangeableState<FeedUiState.Content>.refresh(
        targetQuery: FeedQuery,
        force: Boolean = false,
        baseData: FeedData = snapshot.data,
    ): ChangedState<FeedUiState> {
        val targetData = baseData.copy(
            query = targetQuery,
            filterSelections = targetQuery.localFilters.withGlobalTopics(
                enabled = baseData.excludeTopicsGlobal,
                topics = baseData.globalBlockedTopics,
            ),
        )
        val requestChangesContent = targetQuery != snapshot.displayedQuery ||
                targetData.filterSelections != snapshot.data.filterSelections ||
                targetData.blockedTags != snapshot.data.blockedTags
        if (!force && !requestChangesContent) {
            return mutate { copy(data = targetData) }
        }

        val generation = loadGeneration.incrementAndGet()
        activeRequest = ActiveFeedRequest(
            query = targetQuery,
            filters = targetData.filterSelections,
            blockedTags = targetData.blockedTags,
        )
        mutate {
            copy(
                data = targetData,
                refreshState = FeedRefreshState.Loading(targetQuery),
            )
        }

        return try {
            val page = loadAndFilter(
                query = targetQuery,
                filters = targetData.filterSelections,
                blockedTags = targetData.blockedTags,
            )
            mutate {
                if (loadGeneration.get() != generation) this else copy(
                    data = targetData,
                    page = page,
                    displayedQuery = targetQuery,
                    isClientFiltered = targetData.isClientFiltered,
                    refreshState = FeedRefreshState.Idle,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            mutate {
                if (loadGeneration.get() != generation) this else copy(
                    data = targetData,
                    refreshState = FeedRefreshState.Failed(
                        targetQuery = targetQuery,
                        error = FeedError.LoadFailed,
                    ),
                )
            }
        }
    }

    private suspend fun loadAndFilter(
        query: FeedQuery,
        filters: FilterSelections,
        blockedTags: Set<String>,
    ): FeedPage {
        val page = loadPage(query)
        return page.copy(
            items = page.items
                .filter { !filters.hasAnySelection || matchesFilters(it, filters) }
                .filter { comic -> blockedTags.isEmpty() || comic.tags.none(blockedTags::contains) },
        )
    }

    private suspend fun loadPage(query: FeedQuery): FeedPage = when (val target = discoveryAction) {
        is DiscoveryAction.Channel -> api.searchComics(
            topic = target.name,
            sort = query.sort,
            page = query.page,
        ).comics.toFeedPage(query.page)

        is DiscoveryAction.Knight -> api.searchComics(
            knightId = target.id,
            sort = query.sort,
            page = query.page,
        ).comics.toFeedPage(query.page)

        is DiscoveryAction.AdvancedSearch -> api.advancedSearch(
            content = target.name,
            categories = emptyList(),
            sort = query.sort,
            page = query.page,
        ).comics.toFeedPage(query.page)

        DiscoveryAction.ToFavourite -> api.getFavouriteComics(
            sort = query.sort,
            page = query.page,
        ).comics.toFeedPage(query.page)

        DiscoveryAction.ToRecent -> api.searchComics(
            sort = SortOrder.NEWEST,
            page = query.page,
        ).comics.toFeedPage(query.page)

        DiscoveryAction.ToCollections -> {
            val items = api.getCollections().collections.firstOrNull()?.comics.orEmpty()
            FeedPage(items, page = 1, totalPages = 1, totalCount = items.size)
        }

        DiscoveryAction.ToRandom -> {
            val items = api.getRandomComics().comics
            FeedPage(items, page = 1, totalPages = 1, totalCount = items.size)
        }
    }
}

class FeedStateMachineFactory @Inject constructor(
    private val api: BikaDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource,
) {
    fun create(action: DiscoveryAction): FeedStateMachine = FeedStateMachine(
        api = api,
        userPreferencesDataSource = userPreferencesDataSource,
        discoveryAction = action,
    )
}

private data class ActiveFeedRequest(
    val query: FeedQuery,
    val filters: FilterSelections,
    val blockedTags: Set<String>,
)

private val FeedData.isClientFiltered: Boolean
    get() = filterSelections.hasAnySelection || blockedTags.isNotEmpty()

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

private fun com.shizq.bika.core.network.model.PageData<ComicSummary>.toFeedPage(
    requestedPage: Int,
): FeedPage = FeedPage(
    items = docs,
    page = requestedPage.coerceAtLeast(1),
    totalPages = pages.coerceAtLeast(1),
    totalCount = total,
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
