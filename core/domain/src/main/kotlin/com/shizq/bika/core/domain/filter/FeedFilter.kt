package com.shizq.bika.core.domain.filter

/**
 * 漫画列表的筛选模型。
 *
 * 设计约束：筛选条件**必须携带结构化数据**（如区间的 min/max），展示文案只能出现在 [FilterOption.label]。
 * 旧实现把区间编码进中文标签（"指定数量: >= 5 页"）再反向解析，文案一改筛选就静默失效，
 * 且无法本地化。这里把两者彻底分开：匹配逻辑只读数据字段，永不解析 label。
 */
sealed interface FilterGroup {
    /** 分组标题，如"主题"。 */
    val label: String

    /** 该分组的预设可选项。用户自定义项（见 [FilterOption.CountRange]）不在此列表内。 */
    val options: List<FilterOption>

    data object Topic : FilterGroup {
        override val label: String = "主题"
        override val options: List<FilterOption> = TOPIC_OPTIONS
    }

    data object ExcludeTopic : FilterGroup {
        override val label: String = "排除主题"
        override val options: List<FilterOption> = TOPIC_OPTIONS
    }

    data object Status : FilterGroup {
        override val label: String = "状态"
        override val options: List<FilterOption> = listOf(
            FilterOption.Status(finished = true, label = "完结"),
            FilterOption.Status(finished = false, label = "连载"),
        )
    }

    /** 章节数区间（间接体现连载时间跨度）。 */
    data object EpsRange : FilterGroup {
        override val label: String = "话数"
        override val options: List<FilterOption> = listOf(
            countRange(1, 1, "单话 (1话)"),
            countRange(2, 5, "短篇 (2-5话)"),
            countRange(6, 20, "中篇 (6-20话)"),
            countRange(21, 100, "长篇 (21-100话)"),
            countRange(101, null, "超长篇 (100话以上)"),
        )
    }

    /**
     * 页数区间。
     *
     * 注意：列表接口通常不返回 pagesCount（缺省为 0），此分组对大部分数据不生效。
     * 详见 [matchesFilters] 中的说明。
     */
    data object PagesRange : FilterGroup {
        override val label: String = "页数"
        override val options: List<FilterOption> = listOf(
            countRange(1, 49, "少页 (<50页)"),
            countRange(50, 200, "中等 (50-200页)"),
            countRange(201, 500, "多页 (200-500页)"),
            countRange(501, null, "超多页 (500页以上)"),
        )
    }

    companion object {
        /** UI 展示顺序。 */
        val all: List<FilterGroup> = listOf(Topic, ExcludeTopic, Status, EpsRange, PagesRange)
    }
}

sealed interface FilterOption {
    val label: String

    /** 主题分类，匹配 ComicSummary.categories。 */
    data class Topic(val name: String) : FilterOption {
        override val label: String get() = name
    }

    data class Status(val finished: Boolean, override val label: String) : FilterOption

    /**
     * 闭区间 [[min], [max]]，null 表示该侧无界。
     * 预设项和用户自定义项共用此类型，区别仅在 [label]。
     */
    data class CountRange(
        val min: Int?,
        val max: Int?,
        override val label: String,
    ) : FilterOption {
        init {
            require(min != null || max != null) { "CountRange 至少需要一侧边界" }
        }

        operator fun contains(value: Int): Boolean =
            (min == null || value >= min) && (max == null || value <= max)
    }
}

private fun countRange(min: Int?, max: Int?, label: String) =
    FilterOption.CountRange(min, max, label)

/** 由用户输入的 min/max 构造自定义页数项，label 在此统一生成，UI 不再自行拼接。 */
fun customPagesRange(min: Int?, max: Int?): FilterOption.CountRange? {
    if (min == null && max == null) return null
    val label = when {
        min != null && max != null -> "指定数量: $min - $max 页"
        min != null -> "指定数量: >= $min 页"
        else -> "指定数量: <= $max 页"
    }
    return FilterOption.CountRange(min, max, label)
}

private val TOPIC_OPTIONS: List<FilterOption> = listOf(
    "全彩", "長篇", "同人", "短篇", "圓神領域", "碧藍幻想", "CG雜圖", "英語 ENG",
    "生肉", "純愛", "百合花園", "耽美花園", "偽娘哲學", "後宮閃光", "扶他樂園",
    "單行本", "姐姐系", "妹妹系", "SM", "性轉換", "足の恋", "人妻", "NTR", "強暴",
    "非人類", "艦隊收藏", "Love Live", "SAO 刀劍神域", "Fate", "東方", "WEBTOON",
    "禁書目錄", "歐美", "Cosplay", "重口地帶",
).map(FilterOption::Topic)
