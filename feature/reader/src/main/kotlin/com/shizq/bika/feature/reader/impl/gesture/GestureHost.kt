package com.shizq.bika.feature.reader.impl.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.shizq.bika.core.model.reader.TapAction
import com.shizq.bika.core.model.reader.TapZoneLayout

@Stable
class GestureState(
    private val tapZoneLayout: TapZoneLayout,
    private val isRtl: Boolean,
) {
    fun calculateAction(offset: Offset, size: IntSize): TapAction = tapZoneLayout.resolve(
        x = offset.x,
        y = offset.y,
        width = size.width,
        height = size.height,
        isRtl = isRtl,
    )
}

/**
 * [isRtl] 没有默认值：漏传会让 RTL（日漫）模式的点击方向反向，
 * 这类缺陷在运行时不会报错，只能靠强制显式传入来防。
 * 调用方应直接传 [com.shizq.bika.core.model.reader.ReadingMode.isRtl]。
 */
@Composable
fun rememberGestureState(
    layout: TapZoneLayout,
    isRtl: Boolean,
): GestureState = remember(layout, isRtl) {
    GestureState(
        tapZoneLayout = layout,
        isRtl = isRtl,
    )
}
