package com.shizq.bika.core.download.di

import com.shizq.bika.core.download.executor.ChapterDownloadExecutor
import com.shizq.bika.core.download.executor.DefaultChapterDownloadExecutor
import com.shizq.bika.core.download.monitor.AndroidNetworkMonitor
import com.shizq.bika.core.download.monitor.NetworkMonitor
import com.shizq.bika.core.download.scheduler.DownloadScheduler
import com.shizq.bika.core.download.scheduler.WorkManagerDownloadScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadRuntimeModule {

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
}