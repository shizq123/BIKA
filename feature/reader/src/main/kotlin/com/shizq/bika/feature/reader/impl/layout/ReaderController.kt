package com.shizq.bika.feature.reader.impl.layout

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
     * 像素级连续滚动能力，null 表示该 viewer 不支持（如 Pager）。
     * 调用方（自动滚动）应据此决定是否展示入口，而不是调用一个可能静默失效的方法。
     */
    val continuousScroller: ContinuousScroller?

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
