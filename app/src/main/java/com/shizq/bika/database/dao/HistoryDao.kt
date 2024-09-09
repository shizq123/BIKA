package com.shizq.bika.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shizq.bika.database.model.HistoryEntity

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(vararg historyEntity: HistoryEntity)

    @Update
    suspend fun updateHistory(vararg historyEntity: HistoryEntity)

    @Delete
    suspend fun deleteHistory(vararg historyEntity: HistoryEntity)

    @Query("DELETE FROM HISTORY")
    suspend fun deleteAllHistory()

    //查询全部
    @get:Query("SELECT * FROM HISTORY ORDER BY TIME DESC")
    val allHistoryEntityLive: LiveData<List<HistoryEntity>>

    //查询第一页
    @get:Query("SELECT * FROM HISTORY ORDER BY TIME DESC LIMIT 0, 20")
    val firstPageHistoryEntityLive: LiveData<List<HistoryEntity>>

    //分页查询
    @Query("SELECT * FROM HISTORY ORDER BY TIME DESC LIMIT :page, 20")
    suspend fun gatAllHistory(page: Int): List<HistoryEntity>

    //根据漫画id查询数据
    @Query("SELECT * FROM HISTORY  WHERE COMIC_OR_GAME_ID = :id LIMIT 1")
    suspend fun gatHistory(vararg id: String): List<HistoryEntity>
}