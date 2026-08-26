package com.shizq.bika.feature.reader.impl.progress

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 阅读进度管理配置
 *
 * @property initialLoadTimeout 等待初始数据加载的超时时间（毫秒）
 * @property restoreTimeout 进度恢复的总超时时间（毫秒）
 * @property retryInterval 恢复重试的间隔时间（毫秒）
 * @property stabilizeDelay 滚动到位后的稳定等待时间（毫秒）
 * @property persistDebounce 进度保存的防抖延迟（毫秒）
 */
data class ProgressConfig(
    val initialLoadTimeout: Duration = 3_000.milliseconds,
    val restoreTimeout: Duration = 15_000.milliseconds,
    val retryInterval: Duration = 100.milliseconds,
    val stabilizeDelay: Duration = 300.milliseconds,
    val persistDebounce: Duration = 1_000.milliseconds
)
