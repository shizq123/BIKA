package com.shizq.bika.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.shizq.bika.core.database.model.HistoryRecordEntity
import com.shizq.bika.core.database.model.HistoryWithReadChapters
import com.shizq.bika.core.database.model.ReadChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    /**
     * 更新或插入历史记录及其已读章节。
     */
    @Transaction
    suspend fun upsertHistoryWithChapters(
        history: HistoryRecordEntity,
        readChapters: List<ReadChapterEntity>
    ) {
        upsertHistoryRecord(history)
        insertReadChapters(readChapters)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistoryRecord(history: HistoryRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadChapters(readChapters: List<ReadChapterEntity>)

    /**
     * 查询所有历史记录及其关联的已读章节。
     */
    @Transaction
    @Query("SELECT * FROM historyRecord ORDER BY lastReadAt DESC")
    fun getHistoriesWithReadChapters(): Flow<List<HistoryWithReadChapters>>

    @Query("SELECT * FROM historyRecord WHERE id = :comicId LIMIT 1")
    suspend fun getHistoryById(comicId: String): HistoryRecordEntity?

    /**
     * 删除单条历史记录及其所有关联的已读章节记录。
     * @param comicId 要删除的历史记录的ID。
     */
    @Transaction
    suspend fun clearHistory(comicId: String) {
        deleteHistoryRecordById(comicId)
        deleteReadChaptersByHistoryId(comicId)
    }

    /**
     * 清空所有历史记录和已读章节记录。
     */
    @Transaction
    suspend fun clearAllHistory() {
        deleteAllHistoryRecords()
        deleteAllReadChapters()
    }

    @Query("DELETE FROM historyRecord WHERE id = :comicId")
    suspend fun deleteHistoryRecordById(comicId: String)
    
    @Query("DELETE FROM readChapters WHERE historyId = :historyId")
    suspend fun deleteReadChaptersByHistoryId(historyId: String)

    @Query("DELETE FROM historyRecord")
    suspend fun deleteAllHistoryRecords()

    @Query("DELETE FROM readChapters")
    suspend fun deleteAllReadChapters()
}