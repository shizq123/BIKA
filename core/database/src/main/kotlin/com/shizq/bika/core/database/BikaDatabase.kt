package com.shizq.bika.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shizq.bika.core.database.dao.HistoryDao
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.dao.RecentSearchQueryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.database.model.HistoryRecordEntity
import com.shizq.bika.core.database.model.ReadChapterEntity
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.database.model.RecentSearchQueryEntity
import com.shizq.bika.core.database.util.InstantConverter

@Database(
    entities = [
        RecentSearchQueryEntity::class,
        HistoryRecordEntity::class,
        ReadChapterEntity::class,
        ReadingHistoryEntity::class,
        ChapterProgressEntity::class,
        DetailedHistory::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class)
internal abstract class BikaDatabase : RoomDatabase() {
    abstract fun recentSearchQueryDao(): RecentSearchQueryDao
    abstract fun historyDao(): HistoryDao
    abstract fun readingHistoryDao(): ReadingHistoryDao
}