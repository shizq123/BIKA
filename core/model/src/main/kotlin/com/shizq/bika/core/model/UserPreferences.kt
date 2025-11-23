package com.shizq.bika.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val readingMode: ReadingMode = ReadingMode.Strip,
    val screenOrientation: ScreenOrientation = ScreenOrientation.Portrait,
    val tapZoneLayout: TapZoneLayout = TapZoneLayout.Sides,
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
    Portrait("竖屏 (感应)"), // 允许上下颠倒
    Landscape("横屏 (感应)"), // 允许左右翻转
    LockPortrait("锁定竖屏"), // 严格竖屏
    LockLandscape("锁定横屏"), // 严格横屏
    ReversePortrait("反向竖屏")
}

enum class ReaderAction {
    NextPage,   // 下一页
    PrevPage,   // 上一页
    ToggleMenu, // 开关菜单
    None        // 无操作
}