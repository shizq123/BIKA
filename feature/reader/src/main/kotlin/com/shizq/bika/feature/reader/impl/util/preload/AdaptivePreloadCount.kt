package com.shizq.bika.feature.reader.impl.util.preload

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 根据翻页速率动态调整预载张数：扫读时提高预载数量，精读时降低，参见 [AdaptivePreloadTracker]。
 *
 * 用 [SystemClock.elapsedRealtime] 而非 `System.currentTimeMillis()` 计时：墙钟可能因用户
 * 手动调整或 NTP 校正而回跳，导致翻页间隔算出负值；elapsedRealtime 基于开机时长，不受此影响。
 */
@Composable
internal fun rememberAdaptivePreloadCount(currentPage: Int, baselineCount: Int): Int {
    val tracker = remember { AdaptivePreloadTracker() }
    var preloadCount by remember(baselineCount) { mutableIntStateOf(baselineCount) }

    LaunchedEffect(currentPage) {
        preloadCount = tracker.onPageChanged(SystemClock.elapsedRealtime(), baselineCount)
    }

    return preloadCount
}
