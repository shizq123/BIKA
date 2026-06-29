package com.shizq.bika.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ─── 历史说明 ──────────────────────────────────────────────────────────────
// schema 1  : readingHistory（5 列），无 downloadTask
// schema 2  : 同 1（无 downloadTask）
// schema 3  : downloadTask 首次出现（基础 15 列，无 errorCode / priority 等）
//             readingHistory 同 schema 1
// schema 4  : readingHistory +categories/pagesCount/epsCount/finished/totalLikes
//             downloadTask 无变化
// schema 5  : readingHistory +isFavourited
//             downloadTask 无变化
// schema 6  : downloadTask +priority/worker_token/next_schedule_at/
//                           errorCode/errorMessage/retryCount/updatedAt
//             以及全部索引；readingHistory 无变化
// schema 7  : identityHash 与 schema 6 相同，结构无变化（no-op 升版本号）
// ──────────────────────────────────────────────────────────────────────────

// schema 1 → 2（readingHistory 无结构变化，历史占位）
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // schema 1 与 schema 2 的 downloadTask 均不存在，readingHistory 结构相同，无需操作
    }
}

// schema 2 → 3：新增 downloadTask 表（与 3.json createSql 完全一致）
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `downloadTask` (
                `id` TEXT NOT NULL,
                `comicId` TEXT NOT NULL,
                `comicTitle` TEXT NOT NULL,
                `coverUrl` TEXT NOT NULL,
                `episodeId` TEXT NOT NULL,
                `episodeTitle` TEXT NOT NULL,
                `episodeOrder` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `progress` INTEGER NOT NULL,
                `totalPages` INTEGER NOT NULL,
                `downloadedPages` INTEGER NOT NULL,
                `localPath` TEXT NOT NULL,
                `isViewed` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `completedAt` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}

// schema 3 → 4：readingHistory 新增 categories/pagesCount/epsCount/finished/totalLikes
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE readingHistory ADD COLUMN `categories` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE readingHistory ADD COLUMN `pagesCount` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE readingHistory ADD COLUMN `epsCount` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE readingHistory ADD COLUMN `finished` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE readingHistory ADD COLUMN `totalLikes` INTEGER NOT NULL DEFAULT 0")
    }
}

// schema 4 → 5：readingHistory 新增 isFavourited
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE readingHistory ADD COLUMN `isFavourited` INTEGER NOT NULL DEFAULT 0")
    }
}

// schema 5 → 6：downloadTask 新增全部扩展列及索引
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloadTask ADD COLUMN `priority` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE downloadTask ADD COLUMN `worker_token` TEXT")
        db.execSQL("ALTER TABLE downloadTask ADD COLUMN `next_schedule_at` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE downloadTask ADD COLUMN `errorCode` TEXT NOT NULL DEFAULT 'NONE'")
        db.execSQL("ALTER TABLE downloadTask ADD COLUMN `errorMessage` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE downloadTask ADD COLUMN `retryCount` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE downloadTask ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        // 用 createdAt 回填 updatedAt，避免全表默认值为 0
        db.execSQL("UPDATE downloadTask SET updatedAt = createdAt WHERE updatedAt = 0")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_downloadTask_status` ON `downloadTask`(`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_downloadTask_priority` ON `downloadTask`(`priority`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_downloadTask_createdAt` ON `downloadTask`(`createdAt`)")
        // 建唯一索引前需确保无重复数据；已有重复行会导致此步失败，
        // 但 schema 5 阶段 downloadTask 尚无业务数据写入，可安全创建。
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_downloadTask_comicId_episodeOrder` " +
                    "ON `downloadTask`(`comicId`, `episodeOrder`)"
        )
    }
}

// schema 6 → 7：identityHash 与 6 完全相同，结构无任何变化，no-op
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 无结构变更，仅版本号递增
    }
}

// ── 跨版本快捷路径（供 Room 在找不到逐步路径时使用） ──────────────────────
// 覆盖从 schema 3 直达 6 的完整 delta，避免用户在 3/4/5 停留时走 fallback 清库
val MIGRATION_3_6 = object : Migration(3, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_3_4.migrate(db)
        MIGRATION_4_5.migrate(db)
        MIGRATION_5_6.migrate(db)
    }
}
