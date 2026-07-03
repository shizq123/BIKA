package com.shizq.bika.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 首页频道的默认清单，作为用户未自定义时的初始值。 */
object DefaultChannels {
    val all: List<Channel> = listOf(
        // --- App 功能区 ---
        Channel("推荐", "ic_bika"),
        Channel("排行榜", "ic_cat_ranking"),
        Channel("留言板", "ic_cat_message_board"),
        Channel("最近更新", "ic_cat_recent"),
        Channel("随机本子", "ic_cat_random"),

        // --- 哔咔特有分区 ---
        Channel("大家都在看", "ic_cat_trending"),
        Channel("大濕推薦", "ic_cat_master_choice"),
        Channel("那年今天", "ic_cat_history"),
        Channel("官方都在看", "ic_cat_staff_pick"),
        Channel("嗶咔漢化", "ic_cat_translated"),

        // --- 漫画属性 ---
        Channel("全彩", "ic_cat_full_color"),
        Channel("長篇", "ic_cat_long"),
        Channel("同人", "ic_cat_doujin"),
        Channel("短篇", "ic_cat_short"),
        Channel("單行本", "ic_cat_tankoubon"),
        Channel("CG雜圖", "ic_cat_cg"),
        Channel("英語 ENG", "ic_cat_english"),
        Channel("生肉", "ic_cat_raw"),
        Channel("WEBTOON", "ic_cat_webtoon"),
        Channel("歐美", "ic_cat_western"),
        Channel("Cosplay", "ic_cat_cosplay"),

        // --- 题材/标签 ---
        Channel("純愛", "ic_cat_vanilla"),
        Channel("百合花園", "ic_cat_yuri"),
        Channel("耽美花園", "ic_cat_yaoi"),
        Channel("偽娘哲學", "ic_cat_crossdress"),
        Channel("後宮閃光", "ic_cat_harem"),
        Channel("扶他樂園", "ic_cat_futanari"),
        Channel("姐姐系", "ic_cat_sister_big"),
        Channel("妹妹系", "ic_cat_sister_little"),
        Channel("SM", "ic_cat_bdsm"),
        Channel("性轉換", "ic_cat_gender_bender"),
        Channel("足の恋", "ic_cat_foot"),
        Channel("人妻", "ic_cat_milf"),
        Channel("NTR", "ic_cat_ntr"),
        Channel("強暴", "ic_cat_forced"),
        Channel("非人類", "ic_cat_monster"),
        Channel("重口地帶", "ic_cat_hardcore"),

        // --- IP ---
        Channel("圓神領域", "ic_cat_madoka"),
        Channel("碧藍幻想", "ic_cat_granblue"),
        Channel("艦隊收藏", "ic_cat_kancolle"),
        Channel("Love Live", "ic_cat_lovelive"),
        Channel("SAO 刀劍神域", "ic_cat_sao"),
        Channel("Fate", "ic_cat_fate"),
        Channel("東方", "ic_cat_touhou"),
        Channel("禁書目錄", "ic_cat_index"),
    )
}

@Serializable
data class Channel(
    @SerialName("displayName")
    val label: String,
    /**
     * 图标的稳定逻辑键，由 [ChannelIconRegistry] 映射为实际 drawable。
     * 该值会被持久化，因此与具体资源名解耦：drawable 重命名不影响存量数据。
     */
    val iconKey: String,
    val isActive: Boolean = true,
)