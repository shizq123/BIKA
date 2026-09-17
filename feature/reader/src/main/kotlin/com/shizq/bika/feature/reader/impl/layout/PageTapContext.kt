package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

/**
 * 一次点击的位置信息。
 *
 * [position] 是**视口坐标**而不是被点页面的局部坐标：点击可能发生在跨页模式的
 * 右半屏，按局部坐标算会把右页的左半边当成「屏幕左侧」，翻页方向就反了。
 *
 * [position] 与 [viewportSize] 必须来自**同一个**节点，即由 [viewportAnchor]
 * 标记的那个。点击分区（`TapZoneLayout.resolve`）是按比例把这个矩形切开的，
 * 所以两者原点不一致时每条分区边界都会平移。
 */
data class PageTapContext(
    val position: Offset,
    val viewportSize: IntSize,
)

/**
 * 「视口是谁」的标记。由**定义视口的那一层**（布局策略自己的根节点）用
 * [viewportAnchor] 挂上，产生点击的叶子据此换算坐标。
 *
 * 之前翻页模式的叶子自己调 `findRootCoordinates()` 取整窗口作为视口，而条漫模式
 * 取的是 LazyColumn 容器。两条路径喂给同一个 `GestureState.calculateAction` 的
 * 参考系不同：翻页模式下阅读器内容区被状态栏 inset / scaffold padding 推下去
 * 多少，分区边界就整体偏多少——点"上半屏"的动作会落到相邻分区去。
 *
 * 叶子不知道自己被谁装着，所以视口归属不能由叶子决定。
 */
@Stable
class ViewportAnchor {
    var coordinates: LayoutCoordinates? by mutableStateOf(null)
        private set

    internal fun attach(value: LayoutCoordinates) {
        coordinates = value
    }
}

/** 把本节点标记为 [anchor] 所指的视口。 */
fun Modifier.viewportAnchor(anchor: ViewportAnchor): Modifier =
    onGloballyPositioned { anchor.attach(it) }
