@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.feed

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.database.dao.ReadingHistoryDao
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
import com.shizq.bika.core.model.preferences.UserPreferences
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.navigation.DiscoveryAction
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.atomic.AtomicLong

/**
 * Feed 的单一状态所有者。
 *
 * 查询、外部偏好投射、网络加载和收藏副作用都在这里处理；Android ViewModel 只负责
 * 把状态机绑定到 viewModelScope。每次加载使用递增代际，防止不响应取消的旧请求回写。
 */
class FeedStateMachine internal constructor(
    private val api: BikaDataSource,
    private val historyDao: ReadingHistoryDao,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val discoveryAction: DiscoveryAction,
) : FlowReduxStateMachineFactory<FeedUiState, FeedAction>() {

    private val loadGeneration = AtomicLong(0)
    private val requestLock = Any()

    @Volatile
    private var activeRequest: ActiveFeedRequest? = null

    @Volatile
    private var latestPreferences: UserPreferences? = null

    init {
        initializeWith { FeedUiState() }

        spec {
            inState<FeedUiState> {
                collectWhileInState(historyDao.getDetailedHistories()) { histories ->
                    mutate { copy(detailedHistories = histories) }
                }

                collectWhileInState(userPreferencesDataSource.userData) { preferences ->
                    latestPreferences = preferences
                    applyPreferences(preferences)
                }

                on<FeedAction.Retry> {
                    val filter = latestPreferences?.filter
                    val effectiveFilters = snapshot.query.localFilters.withGlobalTopics(
                        enabled = filter?.globalTopicBlockEnabled ?: snapshot.excludeTopicsGlobal,
                        topics = filter?.globalBlockedTopics ?: snapshot.globalBlockedTopics,
                    )
                    load(
                        targetQuery = snapshot.query,
                        effectiveFilters = effectiveFilters,
                        blockedTags = filter?.blockedTags ?: snapshot.blockedTags,
                        force = true,
                    )
                }

                on<FeedAction.ChangePage> { action ->
                    val totalPages = (snapshot.content as? FeedContentState.Success)
                        ?.page
                        ?.totalPages
                        ?.coerceAtLeast(1)
                        ?: 1
                    val targetQuery = snapshot.query.copy(
                        page = action.page.coerceIn(1, totalPages),
                    )
                    loadForCurrentPreferences(targetQuery)
                }

                on<FeedAction.ChangeSort> { action ->
                    loadForCurrentPreferences(
                        snapshot.query.copy(page = 1, sort = action.sort),
                    )
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
                    val normalizedName = normalizeFavoriteTagName(action.name)
                        ?: return@onActionEffect
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        renameFavoriteTagInList(tags, action.tag, normalizedName)
                    }
                }

                onActionEffect<FeedAction.MoveFavorite> { action ->
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        moveFavoriteTagInList(tags, action.fromIndex, action.toIndex)
                    }
                }

                onActionEffect<FeedAction.AddCustomFavorite> { action ->
                    val normalizedName = normalizeFavoriteTagName(action.name)
                        ?: return@onActionEffect
                    val tag = FavoriteTag(
                        name = normalizedName,
                        actionType = FeedActionType.AdvancedSearch.storageValue,
                    )
                    userPreferencesDataSource.updateFavoriteTags { tags ->
                        addFavoriteTagToList(tags, tag)
                    }
                }
            }
        }
    }

    private suspend fun ChangeableState<FeedUiState>.applyPreferences(
        preferences: UserPreferences,
    ): ChangedState<FeedUiState> {
        val filter = preferences.filter
        val targetQuery = snapshot.query
        val effectiveFilters = targetQuery.localFilters.withGlobalTopics(
            enabled = filter.globalTopicBlockEnabled,
            topics = filter.globalBlockedTopics,
        )
        val displayedQuery = (snapshot.content as? FeedContentState.Success)?.displayedQuery
        val targetAlreadyLoading = activeRequest == ActiveFeedRequest(
            query = targetQuery,
            effectiveFilters = effectiveFilters,
            blockedTags = filter.blockedTags,
        )
        val shouldReload = !snapshot.preferencesReady ||
                (targetQuery != displayedQuery && !targetAlreadyLoading) ||
                effectiveFilters != snapshot.filterSelections ||
                filter.blockedTags != snapshot.blockedTags

        val preferencesChange = mutate {
            copy(
                query = targetQuery,
                favoriteTags = filter.favoriteTags,
                excludeTopicsGlobal = filter.globalTopicBlockEnabled,
                globalBlockedTopics = filter.globalBlockedTopics,
                blockedTags = filter.blockedTags,
                preferencesReady = true,
            )
        }

        return if (shouldReload) {
            load(
                targetQuery = targetQuery,
                effectiveFilters = effectiveFilters,
                blockedTags = filter.blockedTags,
            )
        } else {
            preferencesChange
        }
    }

    private suspend fun ChangeableState<FeedUiState>.toggleFilter(
        group: FilterGroup,
        option: FilterOption,
    ): ChangedState<FeedUiState> {
        if (group is FilterGroup.ExcludeTopic && snapshot.excludeTopicsGlobal) {
            val topic = (option as? FilterOption.Topic)?.name ?: return noChange()
            userPreferencesDataSource.toggleGlobalExcludedTopic(topic)
            val topics = if (topic in snapshot.globalBlockedTopics) {
                snapshot.globalBlockedTopics - topic
            } else {
                snapshot.globalBlockedTopics + topic
            }
            mutate {
                copy(
                    query = query.copy(page = 1),
                    globalBlockedTopics = topics,
                    preferencesReady = true,
                )
            }
            // userData 的新快照会统一计算 effectiveFilters 并触发加载。
            return noChange()
        }

        val targetQuery = snapshot.query.copy(
            page = 1,
            localFilters = snapshot.query.localFilters.toggle(group, option),
        )
        return loadForCurrentPreferences(targetQuery)
    }

    private suspend fun ChangeableState<FeedUiState>.setGlobalTopicFilter(
        enabled: Boolean,
    ): ChangedState<FeedUiState> {
        val persistedEnabled = latestPreferences?.filter?.globalTopicBlockEnabled
            ?: snapshot.excludeTopicsGlobal
        if (enabled == persistedEnabled) return noChange()

        if (enabled) {
            val localExcluded = snapshot.query.localFilters[FilterGroup.ExcludeTopic]
                .orEmpty()
                .filterIsInstance<FilterOption.Topic>()
                .map { it.name }
            userPreferencesDataSource.enableGlobalTopicBlock(localExcluded)
            mutate {
                copy(
                    query = query.copy(page = 1),
                    excludeTopicsGlobal = true,
                    globalBlockedTopics = localExcluded,
                    preferencesReady = true,
                )
            }
        } else {
            val globalExcluded = latestPreferences?.filter?.globalBlockedTopics
                ?: snapshot.globalBlockedTopics
            userPreferencesDataSource.setExcludeTopicsGlobal(false)
            mutate {
                copy(
                    query = query.copy(
                        page = 1,
                        localFilters = if (globalExcluded.isEmpty()) {
                            query.localFilters - FilterGroup.ExcludeTopic
                        } else {
                            query.localFilters + (
                                    FilterGroup.ExcludeTopic to
                                            globalExcluded.map(FilterOption::Topic)
                                    )
                        },
                    ),
                    excludeTopicsGlobal = false,
                    preferencesReady = true,
                )
            }
        }

        // DataStore 发出的新偏好快照负责启动且只启动一次加载。
        return noChange()
    }

    private suspend fun ChangeableState<FeedUiState>.loadForCurrentPreferences(
        targetQuery: FeedQuery,
    ): ChangedState<FeedUiState> {
        val filter = latestPreferences?.filter
        return load(
            targetQuery = targetQuery,
            effectiveFilters = targetQuery.localFilters.withGlobalTopics(
                enabled = filter?.globalTopicBlockEnabled ?: snapshot.excludeTopicsGlobal,
                topics = filter?.globalBlockedTopics ?: snapshot.globalBlockedTopics,
            ),
            blockedTags = filter?.blockedTags ?: snapshot.blockedTags,
        )
    }

    private suspend fun ChangeableState<FeedUiState>.load(
        targetQuery: FeedQuery,
        effectiveFilters: FilterSelections,
        blockedTags: Set<String>,
        force: Boolean = false,
    ): ChangedState<FeedUiState> {
        val previousContent = snapshot.content
        val request = ActiveFeedRequest(targetQuery, effectiveFilters, blockedTags)
        val generation = synchronized(requestLock) {
            if (!force && activeRequest == request) {
                null
            } else {
                activeRequest = request
                loadGeneration.incrementAndGet()
            }
        } ?: return noChange()
        mutate {
            copy(
                query = targetQuery,
                filterSelections = effectiveFilters,
                content = when (previousContent) {
                    is FeedContentState.Success -> previousContent.copy(
                        refreshState = FeedRefreshState.Loading(targetQuery),
                    )

                    FeedContentState.Initial,
                    FeedContentState.Loading,
                    is FeedContentState.Error -> FeedContentState.Loading
                },
            )
        }

        return try {
            val rawPage = loadPage(targetQuery)
            val visibleItems = rawPage.items
                .filter { comic ->
                    !effectiveFilters.hasAnySelection || matchesFilters(comic, effectiveFilters)
                }
                .filter { comic ->
                    blockedTags.isEmpty() || comic.tags.none { it in blockedTags }
                }

            mutate {
                if (loadGeneration.get() != generation) {
                    this
                } else {
                    val filter = latestPreferences?.filter
                    copy(
                        favoriteTags = filter?.favoriteTags ?: favoriteTags,
                        excludeTopicsGlobal = filter?.globalTopicBlockEnabled
                            ?: excludeTopicsGlobal,
                        globalBlockedTopics = filter?.globalBlockedTopics
                            ?: globalBlockedTopics,
                        blockedTags = filter?.blockedTags ?: blockedTags,
                        preferencesReady = preferencesReady || filter != null,
                        content = FeedContentState.Success(
                            page = rawPage.copy(items = visibleItems),
                            displayedQuery = targetQuery,
                            isClientFiltered = effectiveFilters.hasAnySelection ||
                                    blockedTags.isNotEmpty(),
                        ),
                    )
                }
            }
        } catch (e: CancellationException) {
            clearActiveRequest(generation)
            throw e
        } catch (_: Exception) {
            clearActiveRequest(generation)
            mutate {
                if (loadGeneration.get() != generation) {
                    this
                } else {
                    val filter = latestPreferences?.filter
                    copy(
                        favoriteTags = filter?.favoriteTags ?: favoriteTags,
                        excludeTopicsGlobal = filter?.globalTopicBlockEnabled
                            ?: excludeTopicsGlobal,
                        globalBlockedTopics = filter?.globalBlockedTopics
                            ?: globalBlockedTopics,
                        blockedTags = filter?.blockedTags ?: blockedTags,
                        preferencesReady = preferencesReady || filter != null,
                        content = when (val current = content) {
                            is FeedContentState.Success -> current.copy(
                                refreshState = FeedRefreshState.Failed(
                                    targetQuery = targetQuery,
                                    error = FeedError.LoadFailed,
                                ),
                            )

                            FeedContentState.Initial,
                            FeedContentState.Loading,
                            is FeedContentState.Error ->
                                FeedContentState.Error(FeedError.LoadFailed)
                        },
                    )
                }
            }
        }
    }

    private fun clearActiveRequest(generation: Long) {
        synchronized(requestLock) {
            if (loadGeneration.get() == generation) activeRequest = null
        }
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
            FeedPage(items = items, page = 1, totalPages = 1, totalCount = items.size)
        }

        DiscoveryAction.ToRandom -> {
            val items = api.getRandomComics().comics
            FeedPage(items = items, page = 1, totalPages = 1, totalCount = items.size)
        }
    }
}

class FeedStateMachineFactory @Inject constructor(
    private val api: BikaDataSource,
    private val historyDao: ReadingHistoryDao,
    private val userPreferencesDataSource: UserPreferencesDataSource,
) {
    fun create(action: DiscoveryAction): FeedStateMachine = FeedStateMachine(
        api = api,
        historyDao = historyDao,
        userPreferencesDataSource = userPreferencesDataSource,
        discoveryAction = action,
    )
}

private data class ActiveFeedRequest(
    val query: FeedQuery,
    val effectiveFilters: FilterSelections,
    val blockedTags: Set<String>,
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
): FilterSelections {
    if (!enabled) return this
    return if (topics.isEmpty()) {
        this - FilterGroup.ExcludeTopic
    } else {
        this + (FilterGroup.ExcludeTopic to topics.map(FilterOption::Topic))
    }
}
