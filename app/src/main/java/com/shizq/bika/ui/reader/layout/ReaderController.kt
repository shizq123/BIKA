package com.shizq.bika.ui.reader.layout

/**
 * 用于外部（如 Slider、目录点击）控制阅读器，以及获取当前阅读状态。
 */
interface ReaderController {
    val visibleItemIndex: Int
    val totalPages: Int

    suspend fun nextPage(value: Float)
    suspend fun prevPage(value: Float)
    suspend fun scrollToPage(index: Int)
}