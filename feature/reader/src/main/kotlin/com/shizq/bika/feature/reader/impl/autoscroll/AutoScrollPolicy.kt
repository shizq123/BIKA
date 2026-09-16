package com.shizq.bika.feature.reader.impl.autoscroll

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 速度挡位的可调范围（含端点）。
 *
 * 设置面板的滑块、悬浮面板的加减按钮、驱动循环三处共用它。之前这个 1..10
 * 在三个文件里各写了一遍（ReaderScreen 的私有 val、AutoScrollControlPanel 的
 * `speed > 1`/`speed < 10`、ReadingSettingsBottomSheet 的 `1f..10f`），
 * 改上限时漏一处的表现是"滑块能拖到 12 但按钮点不上去"。
 */
internal val AutoScrollSpeedRange = 1..10

/**
 * 自动滚动的推进节奏与停止判据。
 *
 * 抽成数据类是为了让判定本身可单测：以前这段逻辑是驱动循环里的 `when` 分支 +
 * 一个 `zeroStreak` 局部变量，只能靠真机滚到章节末尾来验证。
 *
 * @property pixelsPerSecondPerStep 每挡对应的速度（像素/秒）。
 *   默认 60 使 1..10 挡在 60Hz 屏上与旧的"像素/帧"实现等速。
 * @property stallProbes 判定到底之前允许的连续零推进次数。图片异步加载时布局
 *   尚未撑开，`scrollBy` 会返回瞬时 0，直接判定到底会让自动滚动在半章处停住。
 * @property stallProbeInterval 零推进后的重探间隔，也是等待下一章衔接的轮询间隔。
 * @property flingSettleTimeout 松手后等惯性停下的上限。正常 fling 远快于此，
 *   这个值只是兜底，避免 isScrollInProgress 因故一直为 true 时自动滚动永不恢复。
 * @property maxFrameGap 单帧时间上限。掉帧、GC 停顿或从零推进重探中回来时，
 *   两次帧回调可能相隔很久，不截断会一帧跳过去一大段画面。
 */
data class AutoScrollPolicy(
    val pixelsPerSecondPerStep: Float = 60f,
    val stallProbes: Int = 3,
    val stallProbeInterval: Duration = 200.milliseconds,
    val flingSettleTimeout: Duration = 3.seconds,
    val maxFrameGap: Duration = 48.milliseconds,
) {
    /** 挡位 → 像素/秒。越界挡位直接夹紧，调用方不必自己校验。 */
    internal fun pixelsPerSecond(speed: Int): Float =
        speed.coerceIn(AutoScrollSpeedRange) * pixelsPerSecondPerStep

    /**
     * 一次推进后该做什么。
     *
     * @param consumed [com.shizq.bika.feature.reader.impl.layout.ContinuousScroller.scrollBy]
     *   实际消费的像素数
     * @param stalls 含本次在内的连续零推进次数（[consumed] > 0 时该参数无意义）
     * @param hasNextChapter 是否存在下一章。到底后有下一章就等衔接，没有才是真读完了。
     */
    internal fun decide(consumed: Float, stalls: Int, hasNextChapter: Boolean): AutoScrollStep =
        when {
            consumed > 0f -> AutoScrollStep.Advanced
            stalls <= stallProbes -> AutoScrollStep.Settling
            hasNextChapter -> AutoScrollStep.AwaitingNextChapter
            else -> AutoScrollStep.ReachedEnd
        }

    companion object {
        val Default = AutoScrollPolicy()
    }
}

/** [AutoScrollPolicy.decide] 的结果。 */
internal enum class AutoScrollStep {
    /** 正常推进，继续下一帧。 */
    Advanced,

    /** 推不动，但可能只是布局未就绪，等一会儿再探。 */
    Settling,

    /** 确实到了当前章末端，等下一章衔接完成后会重新有内容可推。 */
    AwaitingNextChapter,

    /** 到达全书末端，结束会话。 */
    ReachedEnd,
}
