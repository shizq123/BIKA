package com.shizq.bika.sync.di

import com.shizq.bika.core.data.util.SyncManager
import com.shizq.bika.sync.status.StubSyncSubscriber
import com.shizq.bika.sync.status.SyncSubscriber
import com.shizq.bika.sync.status.WorkManagerSyncManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    internal abstract fun bindsSyncStatusMonitor(
        syncStatusMonitor: WorkManagerSyncManager,
    ): SyncManager

    @Binds
    internal abstract fun bindsSyncSubscriber(
        syncSubscriber: StubSyncSubscriber,
    ): SyncSubscriber
}