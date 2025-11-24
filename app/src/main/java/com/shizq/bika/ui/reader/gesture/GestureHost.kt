package com.shizq.bika.ui.reader.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.shizq.bika.core.model.ReaderAction
import com.shizq.bika.core.model.TapZoneLayout
import com.shizq.bika.ui.reader.gesture.ReadingDirection.Ltr
import com.shizq.bika.ui.reader.gesture.ReadingDirection.Rtl

/**
 * 阅读方向
 */
enum class ReadingDirection {
    Ltr, // 从左往右 (漫/韩漫)
    Rtl, // 从右往左 (日漫)
}

@Stable
class GestureState(
    private val tapZoneLayout: TapZoneLayout,
    private val readingDirection: ReadingDirection,
) {
    fun onReaderTap(offset: Offset, size: IntSize): ReaderAction {
        if (size.width <= 0 || size.height <= 0) return ReaderAction.None

        val isRtl = readingDirection == Rtl

        return tapZoneLayout.resolve(
            x = offset.x,
            y = offset.y,
            width = size.width,
            height = size.height,
            isRtl = isRtl
        )
    }
}

@Composable
fun rememberGestureState(
    layout: TapZoneLayout = TapZoneLayout.Sides,
    direction: ReadingDirection = Ltr,
): GestureState {
    return remember(layout, direction) {
        GestureState(
            tapZoneLayout = layout,
            readingDirection = direction,
        )
    }
}