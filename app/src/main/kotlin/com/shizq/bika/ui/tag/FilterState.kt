package com.shizq.bika.ui.tag

import androidx.compose.runtime.Immutable
import com.shizq.bika.core.domain.filter.ComicStatus
import com.shizq.bika.core.domain.filter.IntRangeSpec

@Immutable
data class FilterState(val chips: List<FilterChipState>)

sealed interface FeedFilterChange {
    data class IncludedTopic(val topic: String) : FeedFilterChange
    data class ExcludedTopic(val topic: String) : FeedFilterChange
    data class Status(val status: ComicStatus) : FeedFilterChange
    data class EpisodeRange(val range: IntRangeSpec) : FeedFilterChange
    data class PageRange(val range: IntRangeSpec) : FeedFilterChange
}

@Immutable
data class FilterChipState(
    val group: FilterGroup,
    val options: List<FilterOption>,
    val selected: List<FilterOption>,
) {
    val label: String get() = group.label
    val hasSelection: Boolean get() = selected.isNotEmpty()

    fun changeFor(option: FilterOption): FeedFilterChange = when {
        group is FilterGroup.Topic && option is FilterOption.Topic ->
            FeedFilterChange.IncludedTopic(option.name)

        group is FilterGroup.ExcludeTopic && option is FilterOption.Topic ->
            FeedFilterChange.ExcludedTopic(option.name)

        group is FilterGroup.Status && option is FilterOption.Status ->
            FeedFilterChange.Status(
                if (option.finished) ComicStatus.Finished else ComicStatus.Ongoing
            )

        group is FilterGroup.EpsRange && option is FilterOption.CountRange ->
            FeedFilterChange.EpisodeRange(IntRangeSpec(option.min, option.max))

        group is FilterGroup.PagesRange && option is FilterOption.CountRange ->
            FeedFilterChange.PageRange(IntRangeSpec(option.min, option.max))

        else -> error("Filter option $option does not belong to group $group")
    }
}
