package com.shizq.bika.core.domain.filter

import com.shizq.bika.core.model.ComicSummary

enum class ComicStatus(val finished: Boolean) {
    Ongoing(false),
    Finished(true),
}

data class IntRangeSpec(
    val min: Int? = null,
    val max: Int? = null,
) {
    init {
        require(min != null || max != null) { "IntRangeSpec 至少需要一侧边界" }
        require(min == null || max == null || min <= max) { "区间下界不能大于上界" }
    }

    operator fun contains(value: Int): Boolean =
        (min == null || value >= min) && (max == null || value <= max)
}

/** Feed 页面级筛选条件，与展示模型完全解耦。 */
data class FeedCriteria(
    val includedTopics: Set<String> = emptySet(),
    val excludedTopics: Set<String> = emptySet(),
    val statuses: Set<ComicStatus> = emptySet(),
    val episodeRanges: Set<IntRangeSpec> = emptySet(),
    val pageRanges: Set<IntRangeSpec> = emptySet(),
) {
    val hasAnySelection: Boolean
        get() = includedTopics.isNotEmpty() ||
                excludedTopics.isNotEmpty() ||
                statuses.isNotEmpty() ||
                episodeRanges.isNotEmpty() ||
                pageRanges.isNotEmpty()

    fun clear(): FeedCriteria = FeedCriteria()

    fun toggleIncludedTopic(topic: String): FeedCriteria =
        copy(includedTopics = includedTopics.toggle(topic))

    fun toggleExcludedTopic(topic: String): FeedCriteria =
        copy(excludedTopics = excludedTopics.toggle(topic))

    fun toggleStatus(status: ComicStatus): FeedCriteria =
        copy(statuses = statuses.toggle(status))

    fun toggleEpisodeRange(range: IntRangeSpec): FeedCriteria =
        copy(episodeRanges = episodeRanges.toggle(range))

    fun togglePageRange(range: IntRangeSpec): FeedCriteria =
        copy(pageRanges = pageRanges.toggle(range))
}

/** 持久化的全局内容规则。 */
data class GlobalContentPolicy(
    val blockedTopicsEnabled: Boolean = false,
    val blockedTopics: Set<String> = emptySet(),
    val blockedTags: Set<String> = emptySet(),
)

/** 页面条件与全局规则编译后的不可变过滤策略。 */
data class ComicFilterPolicy(
    val includedTopics: Set<String> = emptySet(),
    val excludedTopics: Set<String> = emptySet(),
    val statuses: Set<ComicStatus> = emptySet(),
    val episodeRanges: Set<IntRangeSpec> = emptySet(),
    val pageRanges: Set<IntRangeSpec> = emptySet(),
    val blockedTags: Set<String> = emptySet(),
) {
    val isEmpty: Boolean
        get() = includedTopics.isEmpty() &&
                excludedTopics.isEmpty() &&
                statuses.isEmpty() &&
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
        if (statuses.isNotEmpty() && statuses.none { it.finished == comic.finished }) {
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
    global: GlobalContentPolicy,
): ComicFilterPolicy = ComicFilterPolicy(
    includedTopics = local.includedTopics,
    excludedTopics = local.excludedTopics +
            if (global.blockedTopicsEnabled) global.blockedTopics else emptySet(),
    statuses = local.statuses,
    episodeRanges = local.episodeRanges,
    pageRanges = local.pageRanges,
    blockedTags = global.blockedTags,
)

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value
