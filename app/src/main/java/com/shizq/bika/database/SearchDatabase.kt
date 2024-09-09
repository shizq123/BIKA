package com.shizq.bika.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Search::class], version = 1, exportSchema = false)
abstract class SearchDatabase: RoomDatabase() {
    abstract val searchDao: SearchDao?

    companion object {
        private var INSTANCE: SearchDatabase? = null

        @Synchronized
        fun getDatabase(context: Context): SearchDatabase? {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.applicationContext, SearchDatabase::class.java, "search_database")
                    .setJournalMode(JournalMode.TRUNCATE)
                    .build()
            }
            return INSTANCE
        }
    }
}