package com.shizq.bika.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shizq.bika.core.database.dao.DownloadTaskDao
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.dao.RecentSearchQueryDao
import com.shizq.bika.core.database.dao.TagDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DownloadTaskEntity
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.database.model.RecentSearchQueryEntity
import com.shizq.bika.core.database.model.TagEntity
import com.shizq.bika.core.database.util.InstantConverter
import com.shizq.bika.core.database.util.StringListConverter

@Database(
    entities = [
        ReadingHistoryEntity::class,
        ChapterProgressEntity::class,
        RecentSearchQueryEntity::class,
        TagEntity::class,
        DownloadTaskEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
@TypeConverters(
    InstantConverter::class,
    StringListConverter::class,
)
internal abstract class BikaDatabase : RoomDatabase() {
    abstract fun recentSearchQueryDao(): RecentSearchQueryDao
    abstract fun readingHistoryDao(): ReadingHistoryDao
    abstract fun tagDao(): TagDao
    abstract fun downloadTaskDao(): DownloadTaskDao
}