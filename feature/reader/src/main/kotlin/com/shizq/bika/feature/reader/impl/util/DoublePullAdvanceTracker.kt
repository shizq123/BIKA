package com.shizq.bika.feature.reader.impl.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.shizq.bika.core.model.reader.Direction
import com.shizq.bika.core.model.reader.ReadingMode

/**
 * 双次下拉跳转下一章的决策结果。
 */
sealed interface DoublePullResult {
    /** 第一次下拉：提示“这个是最后一页了” */
    data object PromptLastPage : DoublePullResult

    /** 第二次下拉：确认跳转下一章 */
    data object TriggerAdvance : DoublePullResult
}

/**
 * 双次下拉跳转下一章跟踪器。
 * 纯逻辑类，无任何 Android 依赖，便于单独测试。
 *
 * @param timeoutMillis 两次下拉之间的有效时间窗口（默认 3000ms）。
 * @param timeProvider 时间戳提供器，单测可注入伪时钟。
 */
class DoublePullAdvanceTracker(
    val timeoutMillis: Long = 3000L,
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
) {
    /** 上次有效拉动的时间戳，0 表示未处于等待第2次的状态。 */
    var lastPullTime: Long = 0L
        private set

    /**
     * 当检测到一次有效的触底拉动动作时调用。
     *
     * @return [DoublePullResult.PromptLastPage] 提示最后一页，或 [DoublePullResult.TriggerAdvance] 触发下一章。
     */
    fun onPull(): DoublePullResult {
        val now = timeProvider()
        return if (lastPullTime > 0L && now - lastPullTime <= timeoutMillis) {
            lastPullTime = 0L // 触发跳转后清空，避免连续误触
            DoublePullResult.TriggerAdvance
        } else {
            lastPullTime = now
            DoublePullResult.PromptLastPage
        }
    }

    /**
     * 重置状态（例如切换章节或离开末页时调用）。
     */
    fun reset() {
        lastPullTime = 0L
    }
}

/**
 * 监听到达章节末尾时的手势下拉/滑动，超过阈值且单次手势只触发一次。
 */
@Composable
fun rememberPullToAdvanceNestedScrollConnection(
    enabled: Boolean,
    readingMode: ReadingMode,
    isAtLastPage: Boolean,
    thresholdPx: Float,
    onPullTriggered: () -> Unit,
): NestedScrollConnection {
    val currentOnPullTriggered by rememberUpdatedState(onPullTriggered)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentIsAtLastPage by rememberUpdatedState(isAtLastPage)
    val currentReadingMode by rememberUpdatedState(readingMode)
    val currentThresholdPx by rememberUpdatedState(thresholdPx)

    var accumulatedPull by remember { mutableFloatStateOf(0f) }
    var triggeredInCurrentGesture by remember { mutableStateOf(false) }

    return remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!currentEnabled || !currentIsAtLastPage || source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }

                // 判断是否朝着“下一页 / 下一章”的方向过卷
                val pullDelta = when (currentReadingMode.direction) {
                    Direction.Vertical -> {
                        // 竖向模式（条漫、竖向单页等）：手指向上拉（查看下方内容），available.y 为负数
                        if (available.y < 0f) -available.y else 0f
                    }
                    Direction.Horizontal -> {
                        if (currentReadingMode.isRtl) {
                            // 从右向左翻页（日漫反向）：手指往右滑（下一页），available.x 为正数
                            if (available.x > 0f) available.x else 0f
                        } else {
                            // 从左往右翻页：手指往左滑（下一页），available.x 为负数
                            if (available.x < 0f) -available.x else 0f
                        }
                    }
                }

                if (pullDelta > 0f) {
                    accumulatedPull += pullDelta
                    if (accumulatedPull >= currentThresholdPx && !triggeredInCurrentGesture) {
                        triggeredInCurrentGesture = true
                        currentOnPullTriggered()
                    }
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                accumulatedPull = 0f
                triggeredInCurrentGesture = false
                return Velocity.Zero
            }
        }
    }
}
