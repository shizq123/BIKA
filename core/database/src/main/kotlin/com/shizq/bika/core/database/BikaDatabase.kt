package com.shizq.bika.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.dao.RecentSearchQueryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.database.util.InstantConverter

@Database(
    entities = [
        ReadingHistoryEntity::class,
        ChapterProgressEntity::class,
        DetailedHistory::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class)
internal abstract class BikaDatabase : RoomDatabase() {
    abstract fun recentSearchQueryDao(): RecentSearchQueryDao
    abstract fun readingHistoryDao(): ReadingHistoryDao
}