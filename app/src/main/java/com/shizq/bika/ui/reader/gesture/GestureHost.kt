package com.shizq.bika.ui.reader.gesture

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

@Composable
fun rememberGestureState(
    centerRatio: Float = 0.33f,
    readingDirection: ReadingDirection = ReadingDirection.StartToEnd
): GestureState {
    return remember(centerRatio, readingDirection) {
        GestureState(centerRatio, readingDirection)
    }
}

fun Modifier.readerControls(
    state: GestureState,
    onTap: (GestureAction) -> Unit
): Modifier = pointerInput(state) {
    detectTapGestures { offset ->
        val action = state.calculateAction(offset, size)
        onTap(action)
    }
}

/**
 * 漫画手势动作
 */
enum class GestureAction {
    PrevPage,
    NextPage,
    ToggleMenu,
    None
}

/**
 * 漫画阅读模式
 */
enum class ReadingDirection {
    StartToEnd, // 从左往右读 (条漫)
    EndToStart, // 从右往左读 (日漫)
}

@Stable
class GestureState(
    initialCenterRatio: Float = 0.33f,
    initialDirection: ReadingDirection = ReadingDirection.StartToEnd
) {
    /**
     * 中间区域宽度占比 (0.0 - 1.0)
     */
    var centerZoneRatio: Float by mutableFloatStateOf(initialCenterRatio)

    /**
     * 阅读方向，决定左右点击是上一页还是下一页
     */
    var readingDirection: ReadingDirection by mutableStateOf(initialDirection)

    /**
     * 核心逻辑：根据点击坐标和容器大小，判断意图
     */
    fun calculateAction(offset: Offset, size: IntSize): GestureAction {
        if (size.width == 0) return GestureAction.None

        val x = offset.x
        val width = size.width.toFloat()
        val normalizedX = x / width

        // 计算中间区域的边界
        // 例如 ratio = 0.3, 则中间区域为 [0.35, 0.65]
        val sideZoneRatio = (1f - centerZoneRatio) / 2f
        val rightBoundary = 1f - sideZoneRatio

        return when {
            // 点击中间
            normalizedX in sideZoneRatio..rightBoundary -> GestureAction.ToggleMenu

            // 点击左侧区域
            normalizedX < sideZoneRatio -> {
                if (readingDirection == ReadingDirection.EndToStart) {
                    GestureAction.NextPage // RTL模式下，点击左边是下一页
                } else {
                    GestureAction.PrevPage // LTR模式下，点击左边是上一页
                }
            }

            // 点击右侧区域
            else -> {
                if (readingDirection == ReadingDirection.EndToStart) {
                    GestureAction.PrevPage // RTL模式下，点击右边是上一页
                } else {
                    GestureAction.NextPage // LTR模式下，点击右边是下一页
                }
            }
        }
    }
}