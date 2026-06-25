package com.shizq.bika.core.download.di

import com.shizq.bika.core.download.executor.ChapterDownloadExecutor
import com.shizq.bika.core.download.executor.DefaultChapterDownloadExecutor
import com.shizq.bika.core.download.monitor.AndroidNetworkMonitor
import com.shizq.bika.core.download.monitor.NetworkMonitor
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.repository.RoomDownloadTaskRepository
import com.shizq.bika.core.download.scheduler.DefaultDownloadQueuePolicy
import com.shizq.bika.core.download.scheduler.DownloadQueuePolicy
import com.shizq.bika.core.download.scheduler.DownloadScheduler
import com.shizq.bika.core.download.scheduler.DownloadWorkController
import com.shizq.bika.core.download.scheduler.WorkManagerDownloadScheduler
import com.shizq.bika.core.download.scheduler.WorkManagerDownloadWorkController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.Clock

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadRuntimeModule {
    companion object {
        @Provides
        fun provideClock(): Clock = Clock.System
    }

    @Binds
    abstract fun bindNetworkMonitor(
        impl: AndroidNetworkMonitor,
    ): NetworkMonitor

    @Binds
    abstract fun bindChapterDownloadExecutor(
        impl: DefaultChapterDownloadExecutor,
    ): ChapterDownloadExecutor

    @Binds
    abstract fun bindDownloadScheduler(
        impl: WorkManagerDownloadScheduler,
    ): DownloadScheduler

    @Binds
    abstract fun bindDownloadTaskRepository(
        impl: RoomDownloadTaskRepository,
    ): DownloadTaskRepository

    @Binds
    abstract fun bindDownloadWorkController(
        impl: WorkManagerDownloadWorkController,
    ): DownloadWorkController

    @Binds
    abstract fun bindDownloadQueuePolicy(
        impl: DefaultDownloadQueuePolicy,
    ): DownloadQueuePolicy
}