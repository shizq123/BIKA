package com.shizq.bika.feature.reader.impl.progress

import com.shizq.bika.feature.reader.impl.ReadingProgressStore
import jakarta.inject.Inject
import jakarta.inject.Singleton

/**
 * ProgressManager 工厂
 * 
 * 通过 Hilt 注入依赖，创建 ProgressManager 实例
 */
@Singleton
class ProgressManagerFactory @Inject constructor(
    private val progressSaver: ReadingProgressStore
) {
    fun create(): ProgressManager {
        val asyncStrategy = AsyncSaveStrategy(progressSaver)
        val immediateStrategy = ImmediateSaveStrategy(progressSaver)
        return ProgressManager(asyncStrategy, immediateStrategy)
    }
}
