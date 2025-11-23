package com.shizq.bika.core.model

/**
 * 定义阅读器的布局和行为模式.
 *
 * @param label 用于在UI设置中显示的名称.
 * @param direction 页面布局的主要方向 (水平或垂直).
 * @param viewerType 使用的视图组件类型 (单页翻页器或连续滚动列表).
 * @param isRtl 是否为从右向左阅读 (仅对水平方向有效).
 * @param hasPageGap 在连续滚动模式下，页面之间是否有间隙.
 */
enum class ReadingMode(
    val label: String,
    val direction: Direction,
    val viewerType: ViewerType,
    val isRtl: Boolean = false,
    val hasPageGap: Boolean = false
) {
    /** 横向单页 (左 -> 右) */
    LEFT_TO_RIGHT(
        label = "单页式（从左到右）",
        direction = Direction.Horizontal,
        viewerType = ViewerType.Pager,
        isRtl = false
    ),

    /** 横向单页 (右 -> 左), 日漫模式 */
    RIGHT_TO_LEFT(
        label = "单页式（从右到左）",
        direction = Direction.Horizontal,
        viewerType = ViewerType.Pager,
        isRtl = true
    ),

    /** 竖向单页 (类似抖音, 一页页刷) */
    VERTICAL_PAGER(
        label = "单页式（从上到下）",
        direction = Direction.Vertical,
        viewerType = ViewerType.Pager
    ),

    /** 竖向连续滚动 (无缝长条图) */
    WEBTOON(
        label = "条漫",
        direction = Direction.Vertical,
        viewerType = ViewerType.Scrolling,
        hasPageGap = false
    ),

    /** 竖向连续滚动 (页间有空隙) */
    CONTINUOUS_VERTICAL(
        label = "条漫（页间有空隙）",
        direction = Direction.Vertical,
        viewerType = ViewerType.Scrolling,
        hasPageGap = true
    );

    val isHorizontal: Boolean get() = direction == Direction.Horizontal
}

enum class Direction { Horizontal, Vertical }

enum class ViewerType {
    /** 单页翻页器 */
    Pager,

    /** 连续滚动列表 */
    Scrolling
}