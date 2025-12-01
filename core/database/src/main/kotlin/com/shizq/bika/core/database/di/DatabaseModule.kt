package com.shizq.bika.core.database.di

import android.content.Context
import androidx.room.Room
import com.shizq.bika.core.database.BikaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun providesNiaDatabase(
        @ApplicationContext context: Context,
    ): BikaDatabase = Room.databaseBuilder(
        context,
        BikaDatabase::class.java,
        "bika-database",
    )
        .fallbackToDestructiveMigration(true)
        .build()
}