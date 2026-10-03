package com.shizq.bika.ui.tag

import com.shizq.bika.core.domain.filter.FilterGroup
import com.shizq.bika.core.domain.filter.FilterOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilterStateMappingTest {

    @Test
    fun `映射包含全部筛选分组且保持展示顺序`() {
        val state = emptyMap<FilterGroup, List<FilterOption>>().toFilterState()

        assertEquals(FilterGroup.all, state.chips.map { it.group })
        assertTrue(state.chips.all { !it.hasSelection })
    }

    @Test
    fun `自定义选项会并入可选项且保持选中`() {
        val custom = FilterOption.CountRange(10, 20, "自定义")
        val state = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.PagesRange to listOf(custom),
        ).toFilterState()
        val pagesChip = state.chips.single { it.group == FilterGroup.PagesRange }

        assertTrue(custom in pagesChip.options)
        assertEquals(listOf(custom), pagesChip.selected)
        assertTrue(pagesChip.hasSelection)
    }

    @Test
    fun `预设选项不会被重复加入`() {
        val preset = FilterGroup.Status.options.first()
        val state = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.Status to listOf(preset),
        ).toFilterState()
        val statusChip = state.chips.single { it.group == FilterGroup.Status }

        assertEquals(1, statusChip.options.count { it == preset })
        assertFalse(statusChip.selected.isEmpty())
    }
}
