package com.shizq.bika.core.data.di

import com.shizq.bika.core.data.platform.AndroidAppVersionProvider
import com.shizq.bika.core.data.platform.AndroidUpdateFileProvider
import com.shizq.bika.core.data.platform.AppVersionProvider
import com.shizq.bika.core.data.platform.UpdateFileProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateModule {
    @Binds
    @Singleton
    abstract fun bindAppVersionProvider(
        impl: AndroidAppVersionProvider,
    ): AppVersionProvider

    @Binds
    @Singleton
    abstract fun bindUpdateFileProvider(
        impl: AndroidUpdateFileProvider,
    ): UpdateFileProvider
}