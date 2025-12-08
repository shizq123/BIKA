package com.shizq.bika.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shizq.bika.core.database.dao.HistoryDao
import com.shizq.bika.core.database.dao.RecentSearchQueryDao
import com.shizq.bika.core.database.model.HistoryRecordEntity
import com.shizq.bika.core.database.model.ReadChapterEntity
import com.shizq.bika.core.database.model.RecentSearchQueryEntity
import com.shizq.bika.core.database.util.InstantConverter

@Database(
    entities = [
        RecentSearchQueryEntity::class,
        HistoryRecordEntity::class,
        ReadChapterEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class)
internal abstract class BikaDatabase : RoomDatabase() {
    abstract fun recentSearchQueryDao(): RecentSearchQueryDao
    abstract fun historyDao(): HistoryDao
}