package com.shizq.bika.ui.tag

import androidx.compose.runtime.Immutable

@Immutable
sealed class FilterGroup(open val values: List<String>) {
    @Immutable
    data object Topic : FilterGroup(
        listOf(
            "全彩",
            "長篇",
            "同人",
            "短篇",
            "圓神領域",
            "碧藍幻想",
            "CG雜圖",
            "英語 ENG",
            "生肉",
            "純愛",
            "百合花園",
            "耽美花園",
            "偽娘哲學",
            "後宮閃光",
            "扶他樂園",
            "單行本",
            "姐姐系",
            "妹妹系",
            "SM",
            "性轉換",
            "足の恋",
            "人妻",
            "NTR",
            "強暴",
            "非人類",
            "艦隊收藏",
            "Love Live",
            "SAO 刀劍神域",
            "Fate",
            "東方",
            "WEBTOON",
            "禁書目錄",
            "歐美",
            "Cosplay",
            "重口地帶"
        )
    )

    @Immutable
    data object ExcludeTopic : FilterGroup(Topic.values)

    @Immutable
    data object Status : FilterGroup(listOf("完结"))

    /**
     * 章节数范围筛选（间接体现连载时间跨度）
     * 格式：显示名称，内部通过 matchesFilters 映射到具体 epsCount 区间
     */
    @Immutable
    data object EpsRange : FilterGroup(
        listOf(
            "单话 (1话)",
            "短篇 (2-5话)",
            "中篇 (6-20话)",
            "长篇 (21-100话)",
            "超长篇 (100话以上)"
        )
    )

    /**
     * 页数范围筛选
     * 格式：显示名称，内部通过 matchesFilters 映射到具体 pagesCount 区间
     */
    @Immutable
    data object PagesRange : FilterGroup(
        listOf(
            "少页 (<50页)",
            "中等 (50-200页)",
            "多页 (200-500页)",
            "超多页 (500页以上)"
        )
    )
}