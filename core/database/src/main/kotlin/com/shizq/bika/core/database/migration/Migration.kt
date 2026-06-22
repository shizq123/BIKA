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
        db.execSQL(
            """
            ALTER TABLE download_tasks
            ADD COLUMN priority INTEGER NOT NULL DEFAULT 0
        """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE download_tasks
            ADD COLUMN worker_token TEXT
        """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE download_tasks
            ADD COLUMN next_schedule_at INTEGER NOT NULL DEFAULT 0
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_download_tasks_status_next_priority
            ON download_tasks(status, next_schedule_at, priority DESC, created_at ASC)
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_download_tasks_worker_token
            ON download_tasks(worker_token)
        """.trimIndent()
        )
    }
}