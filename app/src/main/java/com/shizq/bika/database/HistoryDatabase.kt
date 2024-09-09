package com.shizq.bika.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [History::class], version = 1, exportSchema = false)
abstract class HistoryDatabase: RoomDatabase() {
    abstract val historyDao: HistoryDao?

    companion object {
        private var INSTANCE: HistoryDatabase? = null

        @Synchronized
        fun getDatabase(context: Context): HistoryDatabase? {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.applicationContext, HistoryDatabase::class.java, "history_database")
                    .setJournalMode(JournalMode.TRUNCATE)
                    .build()
            }
            return INSTANCE
        }
    }
}