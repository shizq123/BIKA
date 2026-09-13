package com.shizq.bika.feature.reader.impl.progress

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 进度管理配置。
 *
 * 旧配置有 5 个 Duration，其中 restoreTimeout 与 retryInterval 只为算
 * `maxAttempts = restoreTimeout / retryInterval` 而存在，stabilizeDelay 是重试
 * 循环里的经验性等待。恢复改成声明式后这三个都不需要了。
 *
 * @property dataWaitTimeout 等目标页数据真实到位（peek != null）的超时
 * @property confirmTimeout 滚动后等 visibleItemIndex 确认到达的超时
 * @property confirmTolerance 确认到达时容许的页码误差（条漫下高度未测完会差一两项）
 * @property persistDebounce 翻页保存的防抖延迟
 */
data class ProgressConfig(
    val dataWaitTimeout: Duration = 10.seconds,
    val confirmTimeout: Duration = 2.seconds,
    val confirmTolerance: Int = 1,
    val persistDebounce: Duration = 1_000.milliseconds,
)
