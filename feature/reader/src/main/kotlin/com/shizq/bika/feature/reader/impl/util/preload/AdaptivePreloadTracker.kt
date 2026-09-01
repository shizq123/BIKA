package com.shizq.bika.feature.reader.impl.util.preload

/**
 * 维护最近若干次翻页的时间间隔滑动窗口，并据此产出建议的预载张数。
 * 纯状态类，不依赖 Compose，方便单测；由调用方在 Composable 中以
 * `remember { AdaptivePreloadTracker(policy) }` 持有。
 */
class AdaptivePreloadTracker(
    private val policy: AdaptivePreloadPolicy = AdaptivePreloadPolicy(),
) {
    private var lastPageChangeTime: Long = 0L
    private val recentIntervals = mutableListOf<Long>()

    /**
     * 记录一次翻页事件，返回据此更新后建议的预载张数。
     *
     * @param nowMillis 当前时间（毫秒），调用方传入以便单测注入
     * @param baselineCount 用户配置的基准预载张数
     * @return 建议的预载张数
     */
    fun onPageChanged(nowMillis: Long, baselineCount: Int): Int {
        val previous = lastPageChangeTime
        lastPageChangeTime = nowMillis

        if (previous > 0) {
            val interval = nowMillis - previous
            if (policy.isValidInterval(interval)) {
                recentIntervals.add(interval)
                if (recentIntervals.size > policy.sampleWindowSize) {
                    recentIntervals.removeAt(0)
                }
            }
        }

        return policy.resolvePreloadCount(recentIntervals, baselineCount)
    }
}
