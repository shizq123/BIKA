package com.shizq.bika.ui.reader.layout

/**
 * 用于外部（如 Slider、目录点击）控制阅读器，以及获取当前阅读状态。
 */
interface ReaderController {
    val visibleItemIndex: Int
    val totalPages: Int

    suspend fun scrollToPage(index: Int)
    suspend fun scrollBy(value: Float)
    suspend fun animateScrollBy(value: Float)

    suspend fun animateScrollToItem(index: Int)
    suspend fun performFling(initialVelocity: Float)
}