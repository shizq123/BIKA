package com.shizq.bika.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shizq.bika.database.dao.HistoryDao
import com.shizq.bika.database.dao.SearchDao
import com.shizq.bika.database.model.HistoryEntity
import com.shizq.bika.database.model.SearchEntity

@Database(
    entities = [HistoryEntity::class, SearchEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class BikaDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun searchDao(): SearchDao

    companion object {
        @Volatile
        private var instance: BikaDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: buildDatabase(context).also { instance = it }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                BikaDatabase::class.java,
                "bika-database"
            ).build()
    }
}