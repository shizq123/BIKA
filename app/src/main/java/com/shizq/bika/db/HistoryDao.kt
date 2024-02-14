package com.shizq.bika.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistory(vararg history: History)

    @Update
    fun updateHistory(vararg history: History)

    @Delete
    fun deleteHistory(vararg history: History)

    @Query("DELETE FROM HISTORY")
    fun deleteAllHistory()

    //查询全部
    @get:Query("SELECT * FROM HISTORY ORDER BY TIME DESC")
    val allHistoryLive: LiveData<List<History>>

    //查询第一页
    @get:Query("SELECT * FROM HISTORY ORDER BY TIME DESC LIMIT 0,20")
    val firstPageHistoryLive: LiveData<List<History>>

    //分页查询
    @Query("SELECT * FROM HISTORY ORDER BY TIME DESC LIMIT :page,20")
    fun gatAllHistory(vararg page: String): List<History>

    //根据漫画id查询数据
    @Query("SELECT * FROM HISTORY  WHERE COMIC_OR_GAME_ID=:id LIMIT 1")
    fun gatHistory(vararg id: String): List<History>
}