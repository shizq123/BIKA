package com.shizq.bika.ui.dashboard

import com.shizq.bika.core.model.Channel
import com.shizq.bika.navigation.DiscoveryAction

/**
 * 首页频道点击后的去向。
 *
 * 把「频道 → 目标」的判定与实际导航分离，使这段映射可以脱离 Compose 单测；
 * [Unavailable] 让「功能不可用」成为一个可渲染的返回值，而不是从映射函数内部
 * 直接弹 Toast——后者会让一个本该是纯函数的映射持有 Context 并产生副作用。
 */
sealed interface ChannelDestination {
    /** 进入内容流，[action] 决定查询哪一类内容。 */
    data class Feed(val action: DiscoveryAction) : ChannelDestination

    data object Leaderboard : ChannelDestination

    /** 频道当前不可用，[reason] 供 UI 直接呈现。 */
    data class Unavailable(val reason: String) : ChannelDestination
}

/**
 * 按 [Channel.iconKey] 解析目标。
 *
 * 不用 [Channel.label]：label 带 `@SerialName("displayName")`，是可变的展示文案，
 * 改文案或做繁简统一会让 `when` 静默落到 `else` 分支，跳到错误的内容流且不报错。
 * iconKey 是持久化的稳定逻辑键，正是为此存在。
 *
 * `else` 分支把频道名当作哔咔的分区名查询，这是绝大多数频道的正常路径。
 */
fun Channel.toDestination(): ChannelDestination = when (iconKey) {
    ICON_KEY_RECOMMEND -> ChannelDestination.Feed(DiscoveryAction.ToCollections)
    ICON_KEY_RANKING -> ChannelDestination.Leaderboard
    ICON_KEY_RECENT -> ChannelDestination.Feed(DiscoveryAction.ToRecent)
    ICON_KEY_RANDOM -> ChannelDestination.Feed(DiscoveryAction.ToRandom)
    ICON_KEY_MESSAGE_BOARD -> ChannelDestination.Unavailable("该功能已下线")
    else -> ChannelDestination.Feed(DiscoveryAction.Channel(label))
}

private const val ICON_KEY_RECOMMEND = "ic_bika"
private const val ICON_KEY_RANKING = "ic_cat_ranking"
private const val ICON_KEY_RECENT = "ic_cat_recent"
private const val ICON_KEY_RANDOM = "ic_cat_random"
private const val ICON_KEY_MESSAGE_BOARD = "ic_cat_message_board"
