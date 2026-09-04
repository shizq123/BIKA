package com.shizq.bika.domain.filter

import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.RemoteImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedFilterMatchingTest {

    @Test
    fun `空选择集放行所有漫画`() {
        assertTrue(matchesFilters(comic(), emptyMap()))
    }

    @Test
    fun `分组存在但选项为空时该分组不参与匹配`() {
        val selections = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.Status to emptyList()
        )
        assertTrue(matchesFilters(comic(finished = false), selections))
    }

    // region Topic / ExcludeTopic

    @Test
    fun `Topic 同组内取 OR`() {
        val selections = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.Topic to listOf(
                FilterOption.Topic("全彩"),
                FilterOption.Topic("同人"),
            )
        )
        assertTrue(matchesFilters(comic(categories = listOf("同人")), selections))
        assertFalse(matchesFilters(comic(categories = listOf("短篇")), selections))
    }

    @Test
    fun `ExcludeTopic 同组内取 AND 命中任意一个即排除`() {
        val selections = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.ExcludeTopic to listOf(
                FilterOption.Topic("NTR"),
                FilterOption.Topic("重口地帶"),
            )
        )
        assertTrue(matchesFilters(comic(categories = listOf("純愛")), selections))
        assertFalse(matchesFilters(comic(categories = listOf("NTR")), selections))
        assertFalse(
            matchesFilters(comic(categories = listOf("純愛", "重口地帶")), selections)
        )
    }

    @Test
    fun `Topic 与 ExcludeTopic 之间取 AND`() {
        val selections = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.Topic to listOf(FilterOption.Topic("同人")),
            FilterGroup.ExcludeTopic to listOf(FilterOption.Topic("NTR")),
        )
        assertTrue(matchesFilters(comic(categories = listOf("同人")), selections))
        // 命中 Topic 但同时命中排除项
        assertFalse(matchesFilters(comic(categories = listOf("同人", "NTR")), selections))
    }

    // endregion

    // region Status

    @Test
    fun `Status 连载选项能正确匹配未完结漫画`() {
        // 旧实现的 values 里只有"完结"，"连载"分支是死代码，这里锁住修复后的行为
        val ongoing = FilterGroup.Status.options
            .filterIsInstance<FilterOption.Status>()
            .single { !it.finished }
        val selections = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.Status to listOf(ongoing)
        )
        assertTrue(matchesFilters(comic(finished = false), selections))
        assertFalse(matchesFilters(comic(finished = true), selections))
    }

    @Test
    fun `Status 同时选中完结与连载等价于不筛选`() {
        val selections = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.Status to FilterGroup.Status.options
        )
        assertTrue(matchesFilters(comic(finished = true), selections))
        assertTrue(matchesFilters(comic(finished = false), selections))
    }

    // endregion

    // region EpsRange

    @Test
    fun `EpsRange 预设区间边界为闭区间`() {
        val shortRange = FilterOption.CountRange(2, 5, "短篇")
        val selections = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.EpsRange to listOf(shortRange)
        )
        assertFalse(matchesFilters(comic(epsCount = 1), selections))
        assertTrue(matchesFilters(comic(epsCount = 2), selections))
        assertTrue(matchesFilters(comic(epsCount = 5), selections))
        assertFalse(matchesFilters(comic(epsCount = 6), selections))
    }

    @Test
    fun `EpsRange 上界为 null 表示无上限`() {
        val selections = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.EpsRange to listOf(FilterOption.CountRange(101, null, "超长篇"))
        )
        assertFalse(matchesFilters(comic(epsCount = 100), selections))
        assertTrue(matchesFilters(comic(epsCount = 101), selections))
        assertTrue(matchesFilters(comic(epsCount = 9999), selections))
    }

    @Test
    fun `EpsRange 预设项覆盖 1 到 101 无空洞`() {
        val ranges = FilterGroup.EpsRange.options.filterIsInstance<FilterOption.CountRange>()
        (1..101).forEach { count ->
            assertTrue(
                ranges.any { count in it },
                "话数 $count 未被任何预设区间覆盖"
            )
        }
    }

    // endregion

    // region PagesRange

    @Test
    fun `pagesCount 为 0 时放行 因为列表接口不返回该字段`() {
        val selections = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.PagesRange to listOf(FilterOption.CountRange(500, null, "超多页"))
        )
        // 这是有意的降级：若此处返回 false，用户一选页数列表就会整体清空
        assertTrue(matchesFilters(comic(pagesCount = 0), selections))
    }

    @Test
    fun `pagesCount 有值时正常参与筛选`() {
        val selections = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.PagesRange to listOf(FilterOption.CountRange(50, 200, "中等"))
        )
        assertTrue(matchesFilters(comic(pagesCount = 50), selections))
        assertFalse(matchesFilters(comic(pagesCount = 49), selections))
        assertFalse(matchesFilters(comic(pagesCount = 201), selections))
    }

    // endregion

    // region customPagesRange

    @Test
    fun `customPagesRange 双边界`() {
        val range = customPagesRange(10, 20)!!
        assertEquals("指定数量: 10 - 20 页", range.label)
        assertTrue(15 in range)
        assertFalse(21 in range)
    }

    @Test
    fun `customPagesRange 仅下界`() {
        val range = customPagesRange(5, null)!!
        assertEquals("指定数量: >= 5 页", range.label)
        assertTrue(5 in range)
        assertFalse(4 in range)
    }

    @Test
    fun `customPagesRange 仅上界`() {
        val range = customPagesRange(null, 30)!!
        assertEquals("指定数量: <= 30 页", range.label)
        assertTrue(30 in range)
        assertFalse(31 in range)
    }

    @Test
    fun `customPagesRange 两侧均为空返回 null`() {
        assertEquals(null, customPagesRange(null, null))
    }

    @Test
    fun `CountRange 两侧均为 null 时构造失败`() {
        assertFailsWith<IllegalArgumentException> {
            FilterOption.CountRange(null, null, "非法")
        }
    }

    @Test
    fun `自定义区间的匹配不依赖 label 文案`() {
        // 旧实现从 label 反向解析数字，改文案就静默失效。这里用一个完全无关的 label 验证解耦。
        val range = FilterOption.CountRange(min = 10, max = 20, label = "随便写点什么")
        val selections = mapOf<FilterGroup, List<FilterOption>>(
            FilterGroup.PagesRange to listOf(range)
        )
        assertTrue(matchesFilters(comic(pagesCount = 15), selections))
        assertFalse(matchesFilters(comic(pagesCount = 25), selections))
    }

    // endregion

    // region toggle

    @Test
    fun `toggle 选中后再次调用会取消并移除空分组`() {
        val option = FilterOption.Topic("全彩")
        val afterAdd = emptyMap<FilterGroup, List<FilterOption>>()
            .toggle(FilterGroup.Topic, option)
        assertEquals(listOf(option), afterAdd[FilterGroup.Topic])

        val afterRemove = afterAdd.toggle(FilterGroup.Topic, option)
        // 必须移除键而非留下空列表，否则 hasAnySelection 之外的下游会看到"键存在但值为空"
        assertFalse(FilterGroup.Topic in afterRemove)
        assertTrue(afterRemove.isEmpty())
    }

    @Test
    fun `toggle 保留同组其他选项`() {
        val a = FilterOption.Topic("全彩")
        val b = FilterOption.Topic("同人")
        val selections = emptyMap<FilterGroup, List<FilterOption>>()
            .toggle(FilterGroup.Topic, a)
            .toggle(FilterGroup.Topic, b)
            .toggle(FilterGroup.Topic, a)
        assertEquals(listOf(b), selections[FilterGroup.Topic])
    }

    @Test
    fun `hasAnySelection 在所有分组均为空时为 false`() {
        val selections: FilterSelections = mapOf(
            FilterGroup.Topic to emptyList(),
            FilterGroup.Status to emptyList(),
        )
        assertFalse(selections.hasAnySelection)
    }

    // endregion

    private fun comic(
        categories: List<String> = emptyList(),
        finished: Boolean = false,
        epsCount: Int = 1,
        pagesCount: Int = 0,
    ) = ComicSummary(
        id = "test-id",
        title = "测试漫画",
        author = "作者",
        image = RemoteImage(),
        categories = categories,
        finished = finished,
        epsCount = epsCount,
        pagesCount = pagesCount,
    )
}
