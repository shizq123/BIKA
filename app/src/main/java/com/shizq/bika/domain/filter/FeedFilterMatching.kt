package com.shizq.bika.domain.filter

import com.shizq.bika.core.model.ComicSummary

/** 用户已选中的筛选项。空 map 或所有值为空表示不筛选。 */
typealias FilterSelections = Map<FilterGroup, List<FilterOption>>

val FilterSelections.hasAnySelection: Boolean
    get() = values.any { it.isNotEmpty() }

/**
 * 判断 [comic] 是否满足全部已选筛选条件。
 *
 * 语义：分组之间取 AND，同组内多个选项取 OR（[FilterGroup.ExcludeTopic] 例外，同组内也是 AND——
 * 排除多个主题意味着一个都不能命中）。
 */
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

    // pagesCount 缺省为 0：列表接口不返回该字段。此时放行而非过滤掉，
    // 否则一旦用户选中页数区间，整个列表会被清空。
    FilterGroup.PagesRange ->
        comic.pagesCount <= 0 ||
                selected.filterIsInstance<FilterOption.CountRange>().any { comic.pagesCount in it }
}

/** 在 [selections] 中切换 [option] 的选中态，空分组会被移除，避免下游出现"键存在但值为空"。 */
fun FilterSelections.toggle(group: FilterGroup, option: FilterOption): FilterSelections {
    val current = this[group].orEmpty()
    val next = if (option in current) current - option else current + option
    return if (next.isEmpty()) this - group else this + (group to next)
}
