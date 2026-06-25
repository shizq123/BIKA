package com.shizq.bika.core.database.di

import android.content.Context
import androidx.room.Room
import com.shizq.bika.core.database.BikaDatabase
import com.shizq.bika.core.database.migration.MIGRATION_1_2
import com.shizq.bika.core.database.migration.MIGRATION_2_3
import com.shizq.bika.core.database.migration.MIGRATION_3_4
import com.shizq.bika.core.database.migration.MIGRATION_3_6
import com.shizq.bika.core.database.migration.MIGRATION_4_5
import com.shizq.bika.core.database.migration.MIGRATION_5_6
import com.shizq.bika.core.database.migration.MIGRATION_6_7
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
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            // 快捷路径：schema 3 直达 6，Room 会优先选择跨度最大的路径
            MIGRATION_3_6,
        )
        // 仅对 schema 1、2 的旧库走 destructive fallback（这两个版本无 downloadTask，数据可丢弃）
        .fallbackToDestructiveMigrationFrom(true, 1, 2)
        .build()
}