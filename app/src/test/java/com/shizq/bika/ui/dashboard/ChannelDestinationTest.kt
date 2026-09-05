package com.shizq.bika.ui.dashboard

import com.shizq.bika.core.model.Channel
import com.shizq.bika.core.model.DefaultChannels
import com.shizq.bika.navigation.DiscoveryAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ChannelDestinationTest {

    @Test
    fun `推荐 走收藏流`() {
        assertEquals(
            ChannelDestination.Feed(DiscoveryAction.ToCollections),
            Channel("推荐", "ic_bika").toDestination(),
        )
    }

    @Test
    fun `排行榜 走独立页面`() {
        assertEquals(
            ChannelDestination.Leaderboard,
            Channel("排行榜", "ic_cat_ranking").toDestination(),
        )
    }

    @Test
    fun `最近更新 与 随机本子 各走对应内容流`() {
        assertEquals(
            ChannelDestination.Feed(DiscoveryAction.ToRecent),
            Channel("最近更新", "ic_cat_recent").toDestination(),
        )
        assertEquals(
            ChannelDestination.Feed(DiscoveryAction.ToRandom),
            Channel("随机本子", "ic_cat_random").toDestination(),
        )
    }

    @Test
    fun `留言板 返回不可用而非跳转`() {
        val destination = Channel("留言板", "ic_cat_message_board").toDestination()
        assertIs<ChannelDestination.Unavailable>(destination)
    }

    @Test
    fun `未特殊处理的频道按分区名查询`() {
        assertEquals(
            ChannelDestination.Feed(DiscoveryAction.Channel("全彩")),
            Channel("全彩", "ic_cat_full_color").toDestination(),
        )
    }

    /**
     * 这是本次改动要防住的回归：路由曾按 label 匹配，改文案就会静默落到
     * `else` 分支跳错内容流。iconKey 不变时目标必须不变。
     */
    @Test
    fun `改展示文案不影响路由`() {
        assertEquals(
            ChannelDestination.Feed(DiscoveryAction.ToCollections),
            Channel("本子妹推薦", "ic_bika").toDestination(),
        )
        assertEquals(
            ChannelDestination.Leaderboard,
            Channel("排行", "ic_cat_ranking").toDestination(),
        )
    }

    /**
     * 「推荐」的 label 与 [DiscoveryAction.ToCollections.name]（"本子妹推薦"）不同，
     * 一旦按 label 匹配失败就会退化成查一个不存在的分区。其余几个特殊频道的 label
     * 恰好与 action name 相同，失败时表现正常，因此这条最容易漏。
     */
    @Test
    fun `推荐 的 label 与 action name 本就不同`() {
        assertEquals("本子妹推薦", DiscoveryAction.ToCollections.name)
        assertEquals("推荐", DefaultChannels.all.first { it.iconKey == "ic_bika" }.label)
    }

    @Test
    fun `默认频道清单中每个频道都能解析出目标`() {
        DefaultChannels.all.forEach { channel ->
            // toDestination 是穷尽的 when，此处确保没有频道抛异常或落到意外分支
            assertIs<ChannelDestination>(channel.toDestination())
        }
    }
}
