package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.foundation.interaction.InteractionSource
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
     * 是否支持像素级连续滚动。
     *
     * 只有该值为 true 时 [scrollBy] 才有意义，调用方（自动滚动）应据此
     * 决定是否展示入口。Pager 是按页吸附的，无法平滑推进偏移量。
     */
    val supportsContinuousScroll: Boolean

    /**
     * 还能否继续向前滚动（用于自动滚动判断是否触底）。
     */
    val canScrollForward: Boolean

    /**
     * 用户交互源，用于区分「用户手动拖动」与「程序滚动」。
     * 不支持拖动的实现返回一个永不发射的空 source。
     */
    val interactionSource: InteractionSource

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

    /**
     * 平滑移动偏移量（用于自动滚动）。
     *
     * 无默认实现：空默认实现会让不支持的模式静默失效（Pager 曾因此自动滚动
     * 点了没反应）。每个实现必须显式支持或显式拒绝，并同步声明
     * [supportsContinuousScroll]。
     */
    suspend fun scrollBy(value: Float)
}
