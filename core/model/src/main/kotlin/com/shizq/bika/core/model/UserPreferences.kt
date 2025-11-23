package com.shizq.bika.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val readingMode: ReadingMode = ReadingMode.Strip,
    val screenOrientation: ScreenOrientation = ScreenOrientation.Portrait,
    val touchArea: TouchArea = TouchArea.Sides
)

enum class ReadingMode(val label: String) {
    //        SingleLR("单页式（从左到右）"),
//    SingleRL("单页式（从右到左）"),
//    SingleTB("单页式（从上到下）"),
    Strip("条漫"),
//    StripGap("条漫（页间有空隙）")
}

enum class ScreenOrientation(val label: String) {
    System("跟随系统"),
    Portrait("竖屏"),
    Landscape("横屏"),
    LockPortrait("锁定竖屏"),
    LockLandscape("锁定横屏"),
    ReversePortrait("反向竖屏")
}

enum class TouchArea(val label: String) {
    //        LShape("L 形"),
//    Kindle("Kindle"),
    Sides("两侧"),

    //    LeftRight("左右"),
    Off("关闭")
}