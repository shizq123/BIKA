package com.shizq.bika.ui.reader.layout

import kotlinx.coroutines.flow.Flow

/**
 * 阅读器控制接口
 * 屏蔽了底层实现（LazyColumn vs Pager）的差异
 */
interface ReaderController {
    // 总页数（用于进度条显示）
    val totalPages: Int

    val visibleItemIndex: Flow<Int>

    /**
     * 翻到下一页
     * - 条漫模式：向下滚动一屏（通常是高度的 80%）
     * - 翻页模式：切换到 index + 1
     */
    suspend fun scrollNextPage()

    /**
     * 翻到上一页
     */
    suspend fun scrollPrevPage()

    /**
     * 跳转到指定页码（用于目录跳转或进度条拖动）
     */
    suspend fun scrollToPage(index: Int)
}