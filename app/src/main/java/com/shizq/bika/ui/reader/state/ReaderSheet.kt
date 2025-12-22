package com.shizq.bika.ui.reader.state

sealed interface ReaderSheet {
    data object None : ReaderSheet
    data object ChapterList : ReaderSheet     // 侧边目录
    data object Settings : ReaderSheet        // 底部设置
    data object ReadingMode : ReaderSheet     // 底部阅读模式
    data object Orientation : ReaderSheet     // 底部屏幕方向
}