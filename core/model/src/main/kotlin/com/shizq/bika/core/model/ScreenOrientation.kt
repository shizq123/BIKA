package com.shizq.bika.core.model

enum class ScreenOrientation(val label: String) {
    System("跟随系统"),
    Portrait("竖屏 (感应)"), // 允许上下颠倒
    Landscape("横屏 (感应)"), // 允许左右翻转
    LockPortrait("锁定竖屏"), // 严格竖屏
    LockLandscape("锁定横屏"), // 严格横屏
    ReversePortrait("反向竖屏")
}