package com.shizq.bika.ui.tag

import com.shizq.bika.core.domain.filter.ComicStatus
import com.shizq.bika.core.domain.filter.FeedCriteria
import com.shizq.bika.core.domain.filter.IntRangeSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilterStateMappingTest {

    @Test
    fun `映射包含全部筛选分组且保持展示顺序`() {
        val state = FeedCriteria().toFilterState()

        assertEquals(FilterGroup.all, state.chips.map { it.group })
        assertTrue(state.chips.all { !it.hasSelection })
    }

    @Test
    fun `自定义选项会并入可选项且保持选中`() {
        val state = FeedCriteria(
            pageRanges = setOf(IntRangeSpec(10, 20)),
        ).toFilterState()
        val pagesChip = state.chips.single { it.group == FilterGroup.PagesRange }
        val custom = pagesChip.options
            .filterIsInstance<FilterOption.CountRange>()
            .single { it.min == 10 && it.max == 20 }
        assertTrue(custom in pagesChip.options)
        assertEquals(listOf(custom), pagesChip.selected)
        assertTrue(pagesChip.hasSelection)
    }

    @Test
    fun `UI 选项被映射为强类型变更`() {
        val statusChip = FeedCriteria().toFilterState().chips
            .single { it.group == FilterGroup.Status }
        val finished = statusChip.options
            .filterIsInstance<FilterOption.Status>()
            .single { it.finished }

        assertEquals(
            FeedFilterChange.Status(ComicStatus.Finished),
            statusChip.changeFor(finished),
        )
    }

    @Test
    fun `预设选项不会被重复加入`() {
        val preset = FilterGroup.Status.options.first()
        val state = FeedCriteria(
            statuses = setOf(ComicStatus.Finished),
        ).toFilterState()
        val statusChip = state.chips.single { it.group == FilterGroup.Status }

        assertEquals(1, statusChip.options.count { it == preset })
        assertFalse(statusChip.selected.isEmpty())
    }
}
