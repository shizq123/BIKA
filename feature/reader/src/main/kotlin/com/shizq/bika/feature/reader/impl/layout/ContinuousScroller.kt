package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.foundation.interaction.InteractionSource

/**
 * 像素级连续滚动能力。
 *
 * 只有具备该能力的 viewer（[ReaderController.continuousScroller] 非 null）才能被
 * 自动滚动引擎驱动。Pager 按页吸附，无法平滑推进偏移量，其 [ReaderController]
 * 实现直接返回 null——调用方据此判断入口是否展示，不存在"点了没反应"的静默失效。
 */
interface ContinuousScroller {
    /**
     * 尝试向前推进 [pixels] 像素。
     *
     * @return 实际消费的像素数。小于请求值意味着已到达当前内容末端（或被手势中断）。
     */
    suspend fun scrollBy(pixels: Float): Float

    /**
     * 是否仍在滚动（含用户手势产生的 fling）。
     * 用于区分"惯性未停"与"真的停住了"，避免自动滚动在 fling 收尾前抢先启动。
     */
    val isScrollInProgress: Boolean

    /** 用户交互源，用于区分「用户手动拖动」与「程序滚动」。 */
    val interactionSource: InteractionSource
}
