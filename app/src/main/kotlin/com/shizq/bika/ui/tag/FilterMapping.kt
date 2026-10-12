package com.shizq.bika.ui.tag

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.shizq.bika.core.domain.filter.ComicStatus
import com.shizq.bika.core.domain.filter.FeedCriteria
import com.shizq.bika.core.domain.filter.IntRangeSpec

fun FeedCriteria.toFilterState(): FilterState = FilterState(
    chips = FilterGroup.all.map { group ->
        val selected = selectedOptions(group)
        FilterChipState(
            group = group,
            options = group.options + selected.filterNot { it in group.options },
            selected = selected,
        )
    },
)

private fun FeedCriteria.selectedOptions(group: FilterGroup): List<FilterOption> = when (group) {
    FilterGroup.Topic -> includedTopics.map(FilterOption::Topic)
    FilterGroup.ExcludeTopic -> excludedTopics.map(FilterOption::Topic)
    FilterGroup.Status -> statuses.map { status ->
        FilterOption.Status(
            finished = status.finished,
            label = if (status == ComicStatus.Finished) "完结" else "连载",
        )
    }

    FilterGroup.EpsRange -> episodeRanges.map { it.toOption(group) }
    FilterGroup.PagesRange -> pageRanges.map { it.toOption(group) }
}

private fun IntRangeSpec.toOption(group: FilterGroup): FilterOption.CountRange =
    group.options
        .filterIsInstance<FilterOption.CountRange>()
        .firstOrNull { it.min == min && it.max == max }
        ?: FilterOption.CountRange(
            min = min,
            max = max,
            label = when {
                min != null && max != null -> "指定数量: $min - $max"
                min != null -> "指定数量: >= $min"
                else -> "指定数量: <= $max"
            } + if (group == FilterGroup.PagesRange) " 页" else " 话",
        )

@Composable
fun rememberFilterState(criteria: FeedCriteria): FilterState = remember(criteria) {
    criteria.toFilterState()
}
