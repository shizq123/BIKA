package com.shizq.bika.core.model

import kotlinx.serialization.Serializable

object ChannelDataSource {
    private val rawData = listOf(
        // --- App 功能区 ---
        Channel("推荐", "ic_bika"),
        Channel("排行榜", "ic_cat_ranking"),
        Channel("游戏推荐", "ic_cat_game_rec"),
        Channel("哔咔小程序", "ic_cat_mini_app"),
        Channel("哔咔聊天室", "ic_cat_chatroom"),
        Channel("留言板", "ic_cat_message_board"),
        Channel("最近更新", "ic_cat_recent"),
        Channel("随机本子", "ic_cat_random"),

        // --- 哔咔特有分区 ---
        Channel("援助嗶咔", "ic_cat_support"),
        Channel("嗶咔小禮物", "ic_cat_gift"),
        Channel("小電影", "ic_cat_movie"),
        Channel("小里番", "ic_cat_hanime"),
        Channel("嗶咔畫廊", "ic_cat_gallery"),
        Channel("嗶咔商店", "ic_cat_store"),
        Channel("大家都在看", "ic_cat_trending"),
        Channel("大濕推薦", "ic_cat_master_choice"),
        Channel("那年今天", "ic_cat_history"),
        Channel("官方都在看", "ic_cat_staff_pick"),
        Channel("嗶咔運動", "ic_cat_sport"),
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
        Channel("禁書目錄", "ic_cat_index")
    )

    val allChannels: List<Channel> = rawData

    private val lookupMap: Map<String, Channel> = rawData.associateBy { it.displayName }
}

@Serializable
data class Channel(
    val displayName: String,
    val resName: String,
    val isActive: Boolean = true,
)