package com.shizq.bika.core.database.di

import android.content.Context
import androidx.room.Room
import com.shizq.bika.core.database.BikaDatabase
import com.shizq.bika.core.database.migration.MIGRATION_1_2
import com.shizq.bika.core.database.migration.MIGRATION_2_3
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
    fun providesBikaDatabase(
        @ApplicationContext context: Context,
    ): BikaDatabase = Room.databaseBuilder(
        context,
        BikaDatabase::class.java,
        "bika-database",
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        // 仅对无法提供迁移路径的旧版本（1以下）允许破坏性重建，
        // 避免因通配符导致任意版本跳跃都静默丢数据。
        .fallbackToDestructiveMigrationFrom(true, 1)
        .build()
}