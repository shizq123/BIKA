package com.shizq.bika.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shizq.bika.core.database.dao.RecentSearchQueryDao
import com.shizq.bika.core.database.util.InstantConverter

@Database(
    entities = [RecentSearchQueryDao::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class)
internal abstract class BikaDatabase : RoomDatabase() {
    abstract fun recentSearchQueryDao(): RecentSearchQueryDao
}