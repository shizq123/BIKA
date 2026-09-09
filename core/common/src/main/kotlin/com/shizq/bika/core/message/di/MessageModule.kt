package com.shizq.bika.core.message.di

import com.shizq.bika.core.message.MessageReporter
import com.shizq.bika.core.message.MessageSource
import com.shizq.bika.core.message.UserMessageManager
import com.shizq.bika.core.message.UserMessageMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

/**
 * 三个入口指向同一个单例：业务层注入 [MessageReporter]，
 * UI 层注入 [MessageSource]，只有需要两端能力的地方才注入 [UserMessageMonitor]。
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class MessageModule {
    @Binds
    @Singleton
    abstract fun bindsMonitor(impl: UserMessageManager): UserMessageMonitor

    @Binds
    abstract fun bindsReporter(monitor: UserMessageMonitor): MessageReporter

    @Binds
    abstract fun bindsSource(monitor: UserMessageMonitor): MessageSource
}
