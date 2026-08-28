package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * 一次点击的位置信息。
 *
 * [position] 是**视口坐标**而不是被点页面的局部坐标：点击可能发生在跨页模式的
 * 右半屏，按局部坐标算会把右页的左半边当成「屏幕左侧」，翻页方向就反了。
 * 由产生点击的节点负责转换到根坐标系。
 *
 */
data class PageTapContext(
    val position: Offset,
    val viewportSize: IntSize,
)
