package com.shizq.bika.feature.reader.impl.autoscroll

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.shizq.bika.feature.reader.impl.layout.ContinuousScroller
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/** 自动滚动停止的原因，决定调用方除了回写设置之外还要不要提示用户。 */
enum class AutoScrollStopReason {
    /** 用户点了面板上的关闭。 */
    UserClosed,

    /** 已滚到全书末端，没有下一章可衔接。 */
    ReachedEnd,
}

/**
 * 自动滚动的会话态。
 *
 * 语义分两层，混在一起过会导致「关掉了下次进来又自己滚」这类问题：
 * - [isRunning]：本次会话在不在滚。播放/暂停只动它，不落库。
 * - 持久化开关：由 `ReaderConfig.autoScrollEnabled` 表达，只在 [close] 或滚到
 *   全书末端时通过 [AutoScrollStopReason] 回调一次，由调用方 dispatch 回写。
 *
 * 这是个 [Stable] 的持有者而不是每次重组新建的数据类：面板读 [isRunning]，
 * 之前每次重组都拿到一个新实例，任何持有它的 composable 都无法跳过重组。
 */
@Stable
class AutoScrollState internal constructor(initialRunning: Boolean) {

    /** 当前是否正在推进。暂停、被手势打断、到达末端都会变 false。 */
    var isRunning: Boolean by mutableStateOf(initialRunning)
        private set

    /**
     * 当前 viewer 是否具备像素级连续滚动能力（Pager 没有）。
     * 调用方据此决定要不要展示入口，而不是让用户点一个静默失效的按钮。
     */
    var isSupported: Boolean by mutableStateOf(false)
        internal set

    /**
     * 停止回调。普通字段 + [SideEffect] 更新（等价于 rememberUpdatedState 的做法），
     * 保证 [close] 里调到的始终是最近一次组合传入的 lambda。
     */
    internal var onStop: (AutoScrollStopReason) -> Unit = {}

    /** 播放/暂停。只影响本次会话，不回写设置。 */
    fun togglePlayPause() {
        isRunning = !isRunning
    }

    /** 用户主动退出自动滚动：停止并请求回写设置。 */
    fun close() = stop(AutoScrollStopReason.UserClosed)

    private fun stop(reason: AutoScrollStopReason) {
        isRunning = false
        onStop(reason)
    }

    /** 持久化开关变化时同步会话态（如从设置面板打开自动滚动）。 */
    internal fun syncEnabled(enabled: Boolean) {
        isRunning = enabled
    }

    /**
     * 驱动循环，挂起直到被取消或到达全书末端。
     *
     * 按帧推进而不是 `delay(16)`：
     * - 速度单位从「像素/帧」变成「像素/秒」，120Hz 屏上不再是 60Hz 的两倍快；
     * - `withFrameNanos` 与渲染同步，滚动不再因 delay 的调度抖动而顿挫；
     * - 应用退到后台时不再产生帧，循环自然挂起，不用额外挂生命周期观察者。
     *
     * [policy] 在循环启动时取值，运行中改变需要重启本循环（调用方以它为 key）。
     */
    internal suspend fun drive(
        scroller: ContinuousScroller,
        policy: AutoScrollPolicy,
        speed: () -> Int,
        hasNextChapter: () -> Boolean,
    ) {
        val maxGapNanos = policy.maxFrameGap.inWholeNanoseconds
        var stalls = 0
        // 不足 1px 的推进量累积到下一帧再提交：低速 + 高刷下每帧只有半个像素，
        // 直接 scrollBy 可能一次也推不动，会被 stall 判据误当成到达末端。
        var pending = 0f
        var previousFrame = withFrameNanos { it }

        while (true) {
            val frame = withFrameNanos { it }
            // 掉帧、GC 停顿、从 stall 重探回来都会让帧间隔异常大，不截断会一帧跳过一整屏。
            val elapsedNanos = (frame - previousFrame).coerceIn(0L, maxGapNanos)
            previousFrame = frame

            pending += policy.pixelsPerSecond(speed()) * (elapsedNanos / 1_000_000_000f)
            if (pending < 1f) continue

            val consumed = scroller.scrollBy(pending)
            if (consumed > 0f) {
                stalls = 0
                pending -= consumed
            } else {
                stalls++
                // 推不动时清零，否则等章节衔接的这段时间里 pending 会一直累积，
                // 新内容到位的那一帧会突然跳过去一大段。
                pending = 0f
            }

            when (policy.decide(consumed, stalls, hasNextChapter())) {
                AutoScrollStep.Advanced -> Unit

                AutoScrollStep.Settling,
                AutoScrollStep.AwaitingNextChapter,
                    -> {
                    delay(policy.stallProbeInterval)
                    previousFrame = withFrameNanos { it }
                }

                AutoScrollStep.ReachedEnd -> {
                    stop(AutoScrollStopReason.ReachedEnd)
                    return
                }
            }
        }
    }
}

/**
 * 创建并驱动一个 [AutoScrollState]。
 *
 * @param scroller 当前 viewer 的连续滚动能力，null（如 Pager）时返回的 state
 *   恒为 [AutoScrollState.isSupported] = false，不会启动任何循环。
 * @param enabled 持久化设置里的开关。变为 true 时开始滚动，变为 false 时停止。
 * @param speed 速度挡位，取值见 [AutoScrollSpeedRange]。变化即时生效，不重启循环。
 * @param hasNextChapter 是否存在下一章，决定滚到当前章末端时是等衔接还是判定读完。
 * @param onStop 会话结束回调。两种原因都需要回写 `autoScrollEnabled = false`，
 *   合成一个回调是为了不可能只处理其中一条路径。
 */
@Composable
fun rememberAutoScrollState(
    scroller: ContinuousScroller?,
    enabled: Boolean,
    speed: Int,
    hasNextChapter: Boolean,
    onStop: (AutoScrollStopReason) -> Unit,
    policy: AutoScrollPolicy = AutoScrollPolicy.Default,
): AutoScrollState {
    // 不以 scroller 为 key：切章会换一个新 controller，若在这里重建，
    // 用户「暂停后翻到下一章」会莫名恢复滚动。
    val state = remember { AutoScrollState(initialRunning = enabled) }
    SideEffect {
        state.isSupported = scroller != null
        state.onStop = onStop
    }

    LaunchedEffect(state, enabled) { state.syncEnabled(enabled) }

    val isUserInteracting = rememberUserInteracting(scroller, policy.flingSettleTimeout)
    val currentSpeed by rememberUpdatedState(speed)
    val currentHasNextChapter by rememberUpdatedState(hasNextChapter)

    LaunchedEffect(state, scroller, state.isRunning, isUserInteracting, policy) {
        if (scroller == null || !state.isRunning || isUserInteracting) return@LaunchedEffect
        state.drive(
            scroller = scroller,
            policy = policy,
            speed = { currentSpeed },
            hasNextChapter = { currentHasNextChapter },
        )
    }

    return state
}

/**
 * 用户是否正在（或刚刚）操作页面。
 *
 * 松手后要等惯性真正停下，而不是等一个固定延时：自动滚动抢在 fling 收尾前
 * 启动会把用户的甩动生生截断。[settleTimeout] 只是兜底，正常 fling 远快于此。
 */
@Composable
private fun rememberUserInteracting(
    scroller: ContinuousScroller?,
    settleTimeout: Duration,
): Boolean {
    if (scroller == null) return false

    val isDragged by scroller.interactionSource.collectIsDraggedAsState()
    var isInteracting by remember(scroller) { mutableStateOf(false) }

    LaunchedEffect(scroller, isDragged) {
        if (isDragged) {
            isInteracting = true
            return@LaunchedEffect
        }
        withTimeoutOrNull(settleTimeout) {
            snapshotFlow { scroller.isScrollInProgress }.first { !it }
        }
        isInteracting = false
    }

    return isInteracting
}
