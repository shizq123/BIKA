package com.shizq.bika.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE downloadTask ADD COLUMN errorCode TEXT NOT NULL DEFAULT 'NONE'"
        )
        db.execSQL(
            "ALTER TABLE downloadTask ADD COLUMN errorMessage TEXT NOT NULL DEFAULT ''"
        )
        db.execSQL(
            "ALTER TABLE downloadTask ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE downloadTask ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
        )

        // 用 createdAt 回填 updatedAt
        db.execSQL(
            "UPDATE downloadTask SET updatedAt = createdAt WHERE updatedAt = 0"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_downloadTask_status ON downloadTask(status)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_downloadTask_priority ON downloadTask(priority)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_downloadTask_createdAt ON downloadTask(createdAt)"
        )

        // 注意：
        // 如果历史库里存在同 comicId + episodeOrder 的重复数据，
        // 这里创建 unique index 会失败，需先清洗数据。
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_downloadTask_comicId_episodeOrder " +
                    "ON downloadTask(comicId, episodeOrder)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 修复原错误：此前误用 download_tasks（带下划线），实际表名为 downloadTask
        db.execSQL(
            "ALTER TABLE downloadTask ADD COLUMN priority INTEGER NOT NULL DEFAULT 0"
        )

        db.execSQL(
            "ALTER TABLE downloadTask ADD COLUMN worker_token TEXT"
        )

        db.execSQL(
            "ALTER TABLE downloadTask ADD COLUMN next_schedule_at INTEGER NOT NULL DEFAULT 0"
        )

        // 注意：index_downloadTask_priority 已在 MIGRATION_1_2 中创建，此处不重复建立。
        // 沿用与 Entity 一致的索引命名规范（index_<tableName>_<columns>）
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_downloadTask_worker_token ON downloadTask(worker_token)"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_downloadTask_next_schedule_at ON downloadTask(next_schedule_at)"
        )
    }
}