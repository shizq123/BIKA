package com.shizq.bika.core.domain.filter

import com.shizq.bika.core.model.ComicSummary

/**
 * Feed 的强类型筛选条件。领域状态不再以异构 Map 作为事实来源。
 * [FilterSelections] 仅保留为旧 UI 的适配模型。
 */
data class FeedCriteria(
    val includedTopics: Set<String> = emptySet(),
    val excludedTopics: Set<String> = emptySet(),
    val finishedStates: Set<Boolean> = emptySet(),
    val episodeRanges: Set<FilterOption.CountRange> = emptySet(),
    val pageRanges: Set<FilterOption.CountRange> = emptySet(),
) {
    val hasAnySelection: Boolean
        get() = includedTopics.isNotEmpty() ||
                excludedTopics.isNotEmpty() ||
                finishedStates.isNotEmpty() ||
                episodeRanges.isNotEmpty() ||
                pageRanges.isNotEmpty()

    fun clear(): FeedCriteria = FeedCriteria()

    fun toggleIncludedTopic(topic: String): FeedCriteria =
        copy(includedTopics = includedTopics.toggle(topic))

    fun toggleExcludedTopic(topic: String): FeedCriteria =
        copy(excludedTopics = excludedTopics.toggle(topic))

    fun toggleFinishedState(finished: Boolean): FeedCriteria =
        copy(finishedStates = finishedStates.toggle(finished))

    fun toggleEpisodeRange(range: FilterOption.CountRange): FeedCriteria =
        copy(episodeRanges = episodeRanges.toggle(range))

    fun togglePageRange(range: FilterOption.CountRange): FeedCriteria =
        copy(pageRanges = pageRanges.toggle(range))

    /** 旧 UI 兼容入口；新调用方应使用上面的强类型方法。 */
    fun toggle(group: FilterGroup, option: FilterOption): FeedCriteria = when {
        group is FilterGroup.Topic && option is FilterOption.Topic ->
            toggleIncludedTopic(option.name)

        group is FilterGroup.ExcludeTopic && option is FilterOption.Topic ->
            toggleExcludedTopic(option.name)

        group is FilterGroup.Status && option is FilterOption.Status ->
            toggleFinishedState(option.finished)

        group is FilterGroup.EpsRange && option is FilterOption.CountRange ->
            toggleEpisodeRange(option)

        group is FilterGroup.PagesRange && option is FilterOption.CountRange ->
            togglePageRange(option)

        else -> this
    }

    /** 旧通用 FilterChip 的只读 UI 投影。 */
    fun toSelections(): FilterSelections = buildMap {
        putIfNotEmpty(FilterGroup.Topic, includedTopics.map(FilterOption::Topic))
        putIfNotEmpty(FilterGroup.ExcludeTopic, excludedTopics.map(FilterOption::Topic))
        putIfNotEmpty(
            FilterGroup.Status,
            FilterGroup.Status.options
                .filterIsInstance<FilterOption.Status>()
                .filter { it.finished in finishedStates },
        )
        putIfNotEmpty(FilterGroup.EpsRange, episodeRanges.toList())
        putIfNotEmpty(FilterGroup.PagesRange, pageRanges.toList())
    }
}

/** DataStore 全局规则与页面条件编译后的不可变过滤策略。 */
data class ComicFilterPolicy(
    val includedTopics: Set<String> = emptySet(),
    val excludedTopics: Set<String> = emptySet(),
    val finishedStates: Set<Boolean> = emptySet(),
    val episodeRanges: Set<FilterOption.CountRange> = emptySet(),
    val pageRanges: Set<FilterOption.CountRange> = emptySet(),
    val blockedTags: Set<String> = emptySet(),
) {
    val isEmpty: Boolean
        get() = includedTopics.isEmpty() &&
                excludedTopics.isEmpty() &&
                finishedStates.isEmpty() &&
                episodeRanges.isEmpty() &&
                pageRanges.isEmpty() &&
                blockedTags.isEmpty()

    fun matches(comic: ComicSummary): Boolean {
        if (includedTopics.isNotEmpty() && comic.categories.none(includedTopics::contains)) {
            return false
        }
        if (excludedTopics.isNotEmpty() && comic.categories.any(excludedTopics::contains)) {
            return false
        }
        if (finishedStates.isNotEmpty() && comic.finished !in finishedStates) {
            return false
        }
        if (episodeRanges.isNotEmpty() && episodeRanges.none { comic.epsCount in it }) {
            return false
        }
        // 列表接口通常不返回 pagesCount；未知值保持放行。
        if (comic.pagesCount > 0 && pageRanges.isNotEmpty() &&
            pageRanges.none { comic.pagesCount in it }
        ) {
            return false
        }
        if (blockedTags.isNotEmpty() && comic.tags.any(blockedTags::contains)) {
            return false
        }
        return true
    }
}

fun compileFilterPolicy(
    local: FeedCriteria,
    globalTopicBlockEnabled: Boolean,
    globalBlockedTopics: Set<String>,
    blockedTags: Set<String>,
): ComicFilterPolicy = ComicFilterPolicy(
    includedTopics = local.includedTopics,
    excludedTopics = local.excludedTopics +
            if (globalTopicBlockEnabled) globalBlockedTopics else emptySet(),
    finishedStates = local.finishedStates,
    episodeRanges = local.episodeRanges,
    pageRanges = local.pageRanges,
    blockedTags = blockedTags,
)

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value

private fun MutableMap<FilterGroup, List<FilterOption>>.putIfNotEmpty(
    group: FilterGroup,
    options: List<FilterOption>,
) {
    if (options.isNotEmpty()) put(group, options)
}

/** 用户已选中的筛选项。仅作为旧 UI 适配模型。 */
typealias FilterSelections = Map<FilterGroup, List<FilterOption>>

val FilterSelections.hasAnySelection: Boolean
    get() = values.any { it.isNotEmpty() }

/** 兼容旧调用方的匹配入口；新 Feed 应使用 [ComicFilterPolicy.matches]。 */
fun matchesFilters(comic: ComicSummary, selections: FilterSelections): Boolean =
    selections.all { (group, selected) ->
        selected.isEmpty() || matchesGroup(comic, group, selected)
    }

private fun matchesGroup(
    comic: ComicSummary,
    group: FilterGroup,
    selected: List<FilterOption>,
): Boolean = when (group) {
    FilterGroup.Topic ->
        selected.filterIsInstance<FilterOption.Topic>().any { it.name in comic.categories }

    FilterGroup.ExcludeTopic ->
        selected.filterIsInstance<FilterOption.Topic>().none { it.name in comic.categories }

    FilterGroup.Status ->
        selected.filterIsInstance<FilterOption.Status>().any { it.finished == comic.finished }

    FilterGroup.EpsRange ->
        selected.filterIsInstance<FilterOption.CountRange>().any { comic.epsCount in it }

    FilterGroup.PagesRange ->
        comic.pagesCount <= 0 ||
                selected.filterIsInstance<FilterOption.CountRange>().any { comic.pagesCount in it }
}

/** 兼容旧调用方；新 Feed 应使用 [FeedCriteria.toggle]。 */
fun FilterSelections.toggle(group: FilterGroup, option: FilterOption): FilterSelections {
    val current = this[group].orEmpty()
    val next = if (option in current) current - option else current + option
    return if (next.isEmpty()) this - group else this + (group to next)
}
