package com.shizq.bika.core.download.di

import com.shizq.bika.core.download.storage.DefaultLocalComicStorage
import com.shizq.bika.core.download.storage.LocalComicStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadStorageModule {
    @Binds
    abstract fun bindLocalComicStorage(
        impl: DefaultLocalComicStorage,
    ): LocalComicStorage
}