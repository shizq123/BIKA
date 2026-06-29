package com.shizq.bika.core.data.di

import com.shizq.bika.core.data.repository.AppUpdateRepository
import com.shizq.bika.core.data.repository.AppUpdateRepositoryImpl
import com.shizq.bika.core.data.repository.UserRepository
import com.shizq.bika.core.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(impl: AppUpdateRepositoryImpl): AppUpdateRepository
}