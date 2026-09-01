package com.shizq.bika.feature.reader.impl.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 进度条拖动会话的唯一状态来源。
 *
 * [position] 始终有值，等于滑块当前显示的位置；[isScrubbing] 区分“用户正在拖动”
 * 与“滑块跟随阅读进度自动同步”这两种场景。拖动预览卡、Slider 拇指均应读取
 * 同一个 [ScrubState] 实例，避免出现多份互不同步的“拖动到第几页”状态。
 */
@Stable
internal class ScrubState(initialPageIndex: Int) {
    var position by mutableFloatStateOf(initialPageIndex.toFloat())
        private set

    var isScrubbing by mutableStateOf(false)
        private set

    /** 拖动中的目标页（0-based）；未处于拖动状态时为 null。 */
    val previewPageIndex: Int?
        get() = if (isScrubbing) position.toInt() else null

    /** Slider 拖动过程中调用，更新预览位置。 */
    fun onScrub(newPosition: Float) {
        position = newPosition
        isScrubbing = true
    }

    /** 拖动结束（松手），返回落点页码供调用方执行实际滚动。 */
    fun onScrubFinished(): Int {
        isScrubbing = false
        return position.toInt()
    }

    /** 拖动过程中意外中断（如组件被销毁），重置为非拖动状态，不触发跳转。 */
    fun cancelScrub() {
        isScrubbing = false
    }

    /** 阅读进度自然推进时同步滑块位置；拖动进行中不被外部进度打断。 */
    fun syncToPage(pageIndex: Int) {
        if (!isScrubbing) position = pageIndex.toFloat()
    }
}

@Composable
internal fun rememberScrubState(initialPageIndex: Int): ScrubState =
    remember { ScrubState(initialPageIndex) }
