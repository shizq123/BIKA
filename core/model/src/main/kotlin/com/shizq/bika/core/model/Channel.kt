package com.shizq.bika.core.model

object ChannelDataSource {
    private val rawData = listOf(
        // --- App 功能区 ---
        ChannelConfig("推荐", "ic_cat_recommend"),
        ChannelConfig("排行榜", "ic_cat_ranking"),
        ChannelConfig("游戏推荐", "ic_cat_game_rec"),
        ChannelConfig("哔咔小程序", "ic_cat_mini_app"),
        ChannelConfig("哔咔聊天室", "ic_cat_chatroom"),
        ChannelConfig("留言板", "ic_cat_message_board"),
        ChannelConfig("最近更新", "ic_cat_recent"),
        ChannelConfig("随机本子", "ic_cat_random"),

        // --- 哔咔特有分区 ---
        ChannelConfig("援助嗶咔", "ic_cat_support"),
        ChannelConfig("嗶咔小禮物", "ic_cat_gift"),
        ChannelConfig("小電影", "ic_cat_movie"),
        ChannelConfig("小里番", "ic_cat_hanime"),
        ChannelConfig("嗶咔畫廊", "ic_cat_gallery"),
        ChannelConfig("嗶咔商店", "ic_cat_store"),
        ChannelConfig("大家都在看", "ic_cat_trending"),
        ChannelConfig("大濕推薦", "ic_cat_master_choice"),
        ChannelConfig("那年今天", "ic_cat_history"),
        ChannelConfig("官方都在看", "ic_cat_staff_pick"),
        ChannelConfig("嗶咔運動", "ic_cat_sport"),
        ChannelConfig("嗶咔漢化", "ic_cat_translated"),

        // --- 漫画属性 ---
        ChannelConfig("全彩", "ic_cat_full_color"),
        ChannelConfig("長篇", "ic_cat_long"),
        ChannelConfig("同人", "ic_cat_doujin"),
        ChannelConfig("短篇", "ic_cat_short"),
        ChannelConfig("單行本", "ic_cat_tankoubon"),
        ChannelConfig("CG雜圖", "ic_cat_cg"),
        ChannelConfig("英語 ENG", "ic_cat_english"),
        ChannelConfig("生肉", "ic_cat_raw"),
        ChannelConfig("WEBTOON", "ic_cat_webtoon"),
        ChannelConfig("歐美", "ic_cat_western"),
        ChannelConfig("Cosplay", "ic_cat_cosplay"),

        // --- 题材/标签 ---
        ChannelConfig("純愛", "ic_cat_vanilla"),
        ChannelConfig("百合花園", "ic_cat_yuri"),
        ChannelConfig("耽美花園", "ic_cat_yaoi"),
        ChannelConfig("偽娘哲學", "ic_cat_crossdress"),
        ChannelConfig("後宮閃光", "ic_cat_harem"),
        ChannelConfig("扶他樂園", "ic_cat_futanari"),
        ChannelConfig("姐姐系", "ic_cat_sister_big"),
        ChannelConfig("妹妹系", "ic_cat_sister_little"),
        ChannelConfig("SM", "ic_cat_bdsm"),
        ChannelConfig("性轉換", "ic_cat_gender_bender"),
        ChannelConfig("足の恋", "ic_cat_foot"),
        ChannelConfig("人妻", "ic_cat_milf"),
        ChannelConfig("NTR", "ic_cat_ntr"),
        ChannelConfig("強暴", "ic_cat_forced"),
        ChannelConfig("非人類", "ic_cat_monster"),
        ChannelConfig("重口地帶", "ic_cat_hardcore"),

        // --- IP ---
        ChannelConfig("圓神領域", "ic_cat_madoka"),
        ChannelConfig("碧藍幻想", "ic_cat_granblue"),
        ChannelConfig("艦隊收藏", "ic_cat_kancolle"),
        ChannelConfig("Love Live", "ic_cat_lovelive"),
        ChannelConfig("SAO 刀劍神域", "ic_cat_sao"),
        ChannelConfig("Fate", "ic_cat_fate"),
        ChannelConfig("東方", "ic_cat_touhou"),
        ChannelConfig("禁書目錄", "ic_cat_index")
    )

    val allChannels: List<ChannelConfig> = rawData

    private val lookupMap: Map<String, ChannelConfig> = rawData.associateBy { it.displayName }
}

data class ChannelConfig(
    val displayName: String,
    val resName: String,
)