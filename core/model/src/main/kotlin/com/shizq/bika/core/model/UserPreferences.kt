package com.shizq.bika.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val readingMode: ReadingMode = ReadingMode.WEBTOON,
    val screenOrientation: ScreenOrientation = ScreenOrientation.Portrait,
    val tapZoneLayout: TapZoneLayout = TapZoneLayout.Sides,
    val volumeKeyNavigation: Boolean = true,
    val channels: List<Channel> = ChannelDataSource.allChannels,
    val preloadCount: Int = 2,
)