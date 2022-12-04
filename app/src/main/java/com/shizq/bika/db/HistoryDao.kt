package com.shizq.bika.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistory(vararg history: History?)

    @Delete
    fun deleteHistory(vararg history: History?)

    @Query("DELETE FROM HISTORY")
    fun deleteAllHistory()

    @get:Query("SELECT DISTINCT * FROM HISTORY ORDER BY ID DESC")
    val allHistoryLive: LiveData<List<History>>
}