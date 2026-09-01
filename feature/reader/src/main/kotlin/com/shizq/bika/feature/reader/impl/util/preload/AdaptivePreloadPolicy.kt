package com.shizq.bika.feature.reader.impl.util.preload

/**
 * 根据用户最近的翻页速率，动态调整预载张数：
 * 快速扫读时多预载，慢速精读时少预载，避免不必要的带宽/内存开销。
 *
 * @property sampleWindowSize 参与计算平均速率的最近翻页间隔样本数
 * @property minValidInterval 有效翻页间隔下限（毫秒），小于此值视为异常（如连续快速点击）
 * @property maxValidInterval 有效翻页间隔上限（毫秒），大于此值视为异常（如跳章、长时间停留后才翻页）
 * @property fastReadingThreshold 平均间隔小于此值（毫秒）判定为快速扫读
 * @property slowReadingThreshold 平均间隔大于此值（毫秒）判定为慢速精读
 * @property fastReadingPreloadCount 快速扫读时的预载张数
 * @property slowReadingPreloadCount 慢速精读时的预载张数
 */
data class AdaptivePreloadPolicy(
    val sampleWindowSize: Int = 3,
    val minValidInterval: Long = 100,
    val maxValidInterval: Long = 10_000,
    val fastReadingThreshold: Long = 1_500,
    val slowReadingThreshold: Long = 3_200,
    val fastReadingPreloadCount: Int = 6,
    val slowReadingPreloadCount: Int = 2,
) {
    /**
     * 有效翻页间隔的取值范围，超出范围的间隔（如跳章、长时间停留）会被 [isValidInterval] 剔除。
     */
    val validIntervalRange: LongRange get() = minValidInterval..maxValidInterval

    fun isValidInterval(intervalMillis: Long): Boolean = intervalMillis in validIntervalRange

    /**
     * 根据最近若干次翻页间隔的平均值，计算建议的预载张数。
     *
     * @param recentIntervals 最近的翻页间隔样本（毫秒），通常取最后 [sampleWindowSize] 个
     * @param baselineCount 用户在设置中配置的基准预载张数；为 0 表示用户关闭了预载，
     *   此时不做自适应调整，始终返回 0
     * @return 建议的预载张数；样本数不足 [sampleWindowSize] 时返回 [baselineCount]（保持现状）
     */
    fun resolvePreloadCount(recentIntervals: List<Long>, baselineCount: Int): Int {
        if (baselineCount == 0) return 0
        if (recentIntervals.size < sampleWindowSize) return baselineCount

        val average = recentIntervals.average()
        return when {
            average < fastReadingThreshold -> fastReadingPreloadCount
            average > slowReadingThreshold -> slowReadingPreloadCount
            else -> baselineCount
        }
    }
}
