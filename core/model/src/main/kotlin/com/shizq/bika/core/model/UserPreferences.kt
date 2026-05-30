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
    val fontScale: Float = 1.0f,
    val isLoggingEnabled: Boolean = false,
    val downloadOverWifiOnly: Boolean = false,
    val maxConcurrentDownloads: Int = 3,
    // ---- 缓存的用户信息（无网时回退使用）----
    val cachedUserName: String = "",
    val cachedUserAvatarUrl: String = "",
    val cachedUserLevel: Int = 0,
    val cachedUserExp: Int = 0,
    val cachedUserTitle: String = "",
    val cachedUserGender: String = "",
    val cachedUserSlogan: String = "",
    val cachedUserCharacters: List<String> = emptyList(),
)