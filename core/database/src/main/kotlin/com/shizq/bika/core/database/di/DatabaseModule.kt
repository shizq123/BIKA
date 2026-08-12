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
        // 不注册 fallbackToDestructiveMigration：一旦触发会静默清空下载任务、
        // 阅读历史等全部用户数据。升级路径缺失时宁可抛异常崩溃（数据保留在磁盘，
        // 修复迁移路径后即可恢复），也不能无声无息丢数据。每次升级必须补全迁移路径。
        .build()
}