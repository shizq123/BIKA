package com.shizq.bika.feature.reader.impl.autoscroll

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.shizq.bika.feature.reader.impl.layout.ContinuousScroller
import kotlinx.coroutines.delay

/**
 * 自动滚动的会话态：驱动 [ContinuousScroller] 按固定速度持续推进，
 * 直到用户手动交互、到达内容末端或被显式关闭。
 *
 * 播放/暂停（[togglePause]）只影响本次会话，不回写持久化设置；
 * 关闭（[disableAndPersist]）会同时回写设置，保证下次进入阅读器不会又自动开始滚动。
 */
class AutoScrollController internal constructor(
    val isScrolling: Boolean,
    val isSupported: Boolean,
    private val setScrolling: (Boolean) -> Unit,
    private val onDisablePersisted: () -> Unit,
) {
    fun togglePause() = setScrolling(!isScrolling)

    fun disableAndPersist() {
        setScrolling(false)
        onDisablePersisted()
    }
}

/**
 * 创建并驱动一个 [AutoScrollController]。
 *
 * @param scroller 当前 viewer 的连续滚动能力，null 表示不支持（如 Pager），
 *   此时返回的 controller 恒为不可用状态，不会启动任何滚动循环。
 * @param enabled 来自持久化设置的初始开关（如从设置面板打开自动滚动）。
 * @param speed 滚动速度（像素/帧，由设置面板的 1~10 挡位映射而来）。
 * @param hasNextChapter 是否存在下一章，决定到达末端时是等待衔接还是判定"已读完"。
 * @param onReachEnd 到达全书末端（无下一章可衔接）时的回调，用于展示提示。
 * @param onSettingChanged 需要回写持久化设置时的回调（[togglePause] 不触发，
 *   仅 [disableAndPersist] 或到达末端时触发）。
 */
@Composable
fun rememberAutoScrollController(
    scroller: ContinuousScroller?,
    enabled: Boolean,
    speed: Int,
    hasNextChapter: Boolean,
    onReachEnd: () -> Unit,
    onSettingChanged: (Boolean) -> Unit,
): AutoScrollController {
    var isScrolling by remember(enabled) { mutableStateOf(enabled) }

    var isUserInteracting by remember { mutableStateOf(false) }
    if (scroller != null) {
        val isDragged by scroller.interactionSource.collectIsDraggedAsState()
        LaunchedEffect(isDragged) {
            if (isDragged) {
                isUserInteracting = true
            } else {
                // 等待手势 fling 真正停下，而非固定延时，避免自动滚动抢跑打断惯性，
                // 3s 上限只是兜底，正常 fling 远快于此。
                var waited = 0
                while (scroller.isScrollInProgress && waited < 3000) {
                    delay(16)
                    waited += 16
                }
                isUserInteracting = false
            }
        }
    }

    LaunchedEffect(isScrolling, isUserInteracting, speed, scroller, hasNextChapter) {
        if (scroller == null || !isScrolling || isUserInteracting) return@LaunchedEffect
        var zeroStreak = 0
        while (true) {
            val consumed = scroller.scrollBy(speed.toFloat())
            when {
                consumed > 0f -> {
                    zeroStreak = 0
                    delay(16)
                }
                // 连续多次推进为 0 才判定为真的到底，避免内容尚未完成布局
                // （如图片异步加载）时的瞬时 0 被误判为已到达末端。
                zeroStreak < 3 -> {
                    zeroStreak++
                    delay(200)
                }

                hasNextChapter -> delay(200) // 已到当前章末端，等待下一章衔接完成
                else -> {
                    isScrolling = false
                    onSettingChanged(false)
                    onReachEnd()
                    break
                }
            }
        }
    }

    return AutoScrollController(
        isScrolling = isScrolling,
        isSupported = scroller != null,
        setScrolling = { isScrolling = it },
        onDisablePersisted = { onSettingChanged(false) }
    )
}
