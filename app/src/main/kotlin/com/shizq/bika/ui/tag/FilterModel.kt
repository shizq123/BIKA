package com.shizq.bika.ui.tag

/** 仅供筛选控件使用的展示分组。 */
sealed interface FilterGroup {
    val label: String
    val options: List<FilterOption>

    data object Topic : FilterGroup {
        override val label = "主题"
        override val options = TOPIC_OPTIONS
    }

    data object ExcludeTopic : FilterGroup {
        override val label = "排除主题"
        override val options = TOPIC_OPTIONS
    }

    data object Status : FilterGroup {
        override val label = "状态"
        override val options = listOf(
            FilterOption.Status(finished = true, label = "完结"),
            FilterOption.Status(finished = false, label = "连载"),
        )
    }

    data object EpsRange : FilterGroup {
        override val label = "话数"
        override val options = listOf(
            countRange(1, 1, "单话 (1话)"),
            countRange(2, 5, "短篇 (2-5话)"),
            countRange(6, 20, "中篇 (6-20话)"),
            countRange(21, 100, "长篇 (21-100话)"),
            countRange(101, null, "超长篇 (100话以上)"),
        )
    }

    data object PagesRange : FilterGroup {
        override val label = "页数"
        override val options = listOf(
            countRange(1, 49, "少页 (<50页)"),
            countRange(50, 200, "中等 (50-200页)"),
            countRange(201, 500, "多页 (200-500页)"),
            countRange(501, null, "超多页 (500页以上)"),
        )
    }

    companion object {
        val all: List<FilterGroup> = listOf(Topic, ExcludeTopic, Status, EpsRange, PagesRange)
    }
}

sealed interface FilterOption {
    val label: String

    data class Topic(val name: String) : FilterOption {
        override val label: String get() = name
    }

    data class Status(
        val finished: Boolean,
        override val label: String,
    ) : FilterOption

    data class CountRange(
        val min: Int?,
        val max: Int?,
        override val label: String,
    ) : FilterOption
}

private fun countRange(min: Int?, max: Int?, label: String) =
    FilterOption.CountRange(min, max, label)

private val TOPIC_OPTIONS: List<FilterOption> = listOf(
    "全彩", "長篇", "同人", "短篇", "圓神領域", "碧藍幻想", "CG雜圖", "英語 ENG",
    "生肉", "純愛", "百合花園", "耽美花園", "偽娘哲學", "後宮閃光", "扶他樂園",
    "單行本", "姐姐系", "妹妹系", "SM", "性轉換", "足の恋", "人妻", "NTR", "強暴",
    "非人類", "艦隊收藏", "Love Live", "SAO 刀劍神域", "Fate", "東方", "WEBTOON",
    "禁書目錄", "歐美", "Cosplay", "重口地帶",
).map(FilterOption::Topic)
