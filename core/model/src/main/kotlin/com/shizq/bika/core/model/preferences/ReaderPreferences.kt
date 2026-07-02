package com.shizq.bika.core.model.preferences

import com.shizq.bika.core.model.AutoScrollConfig
import com.shizq.bika.core.model.BookSpreadsMode
import com.shizq.bika.core.model.EyeCareConfig
import com.shizq.bika.core.model.reader.ReadingMode
import com.shizq.bika.core.model.reader.ScreenOrientation
import com.shizq.bika.core.model.reader.TapZoneLayout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReaderPreferences(
    val readingMode: ReadingMode = ReadingMode.WEBTOON,
    val screenOrientation: ScreenOrientation = ScreenOrientation.Portrait,
    val tapZoneLayout: TapZoneLayout = TapZoneLayout.Sides,
    @SerialName("volumeKeyNavigation")
    val volumeKeyNavigationEnabled: Boolean = true,
    val preloadCount: Int = 2,
    val eyeCare: EyeCareConfig = EyeCareConfig(),
    val autoScroll: AutoScrollConfig = AutoScrollConfig(),
    val bookSpreadsMode: BookSpreadsMode = BookSpreadsMode.AUTO,
    val magnifierEnabled: Boolean = true,
    val statusBarCapsuleEnabled: Boolean = true,
)