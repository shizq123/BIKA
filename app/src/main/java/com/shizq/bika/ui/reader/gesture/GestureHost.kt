package com.shizq.bika.ui.reader.gesture

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import com.shizq.bika.core.model.ReaderAction
import com.shizq.bika.core.model.TapZoneLayout
import com.shizq.bika.ui.reader.gesture.ReadingDirection.Ltr
import com.shizq.bika.ui.reader.gesture.ReadingDirection.Rtl

@Composable
fun rememberGestureState(
    initialTapZoneLayout: TapZoneLayout = TapZoneLayout.Sides,
    initialDirection: ReadingDirection = ReadingDirection.Ltr,
    onAction: (ReaderAction) -> Unit
): GestureState {
    val latestOnAction by rememberUpdatedState(onAction)

    return remember(initialTapZoneLayout, initialDirection) {
        GestureState(
            initialTapZoneLayout = initialTapZoneLayout,
            initialDirection = initialDirection,
            onAction = { action -> latestOnAction(action) }
        )
    }
}

fun Modifier.readerControls(
    gestureState: GestureState
): Modifier = pointerInput(Unit) {
    detectTapGestures { offset ->
        val size = size
        if (size.width > 0 && size.height > 0) {
            val action = gestureState.calculateAction(offset, size)
            gestureState.onAction(action)
        }
    }
}

/**
 * 阅读方向
 * @property Ltr: 从左往右 (条漫/国漫)
 * @property Rtl: 从右往左 (日漫)
 */
enum class ReadingDirection {
    Ltr,
    Rtl,
}
@Stable
class GestureState(
    initialTapZoneLayout: TapZoneLayout,
    initialDirection: ReadingDirection,
    val onAction: (ReaderAction) -> Unit
) {
    /**
     * 当前点击区域模式 (支持动态修改)
     */
    var tapZoneLayout: TapZoneLayout by mutableStateOf(initialTapZoneLayout)

    /**
     * 当前阅读方向 (支持动态修改)
     */
    var readingDirection: ReadingDirection by mutableStateOf(initialDirection)

    /**
     * 计算点击意图
     * 现在将计算委托给了 touchArea 枚举
     */
    fun calculateAction(offset: Offset, size: IntSize): ReaderAction {
        if (size.width == 0 || size.height == 0) return ReaderAction.None

        // 将 ReadingDirection 转换为布尔值传给算法
        val isRtl = readingDirection == ReadingDirection.Rtl

        return tapZoneLayout.resolve(
            x = offset.x,
            y = offset.y,
            width = size.width,
            height = size.height,
            isRtl = isRtl
        )
    }
}