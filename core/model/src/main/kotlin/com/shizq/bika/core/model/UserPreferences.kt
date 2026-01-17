package com.shizq.bika.core.model

import kotlinx.serialization.Serializable

/**
 * @property selectedNetworkLine 分流线路
 * @property autoCheckIn 是否开启每日自动签到
 */
@Serializable
data class UserPreferences(
    val readingMode: ReadingMode = ReadingMode.WEBTOON,
    val screenOrientation: ScreenOrientation = ScreenOrientation.Portrait,
    val tapZoneLayout: TapZoneLayout = TapZoneLayout.Sides,
    val volumeKeyNavigation: Boolean = true,
    val channels: List<Channel> = ChannelDataSource.allChannels,
    val preloadCount: Int = 2,
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val selectedNetworkLine: NetworkLine = NetworkLine.LINE_1,
    val autoCheckIn: Boolean = true,
    val dns: Set<String> = setOf("104.21.20.188"),
)