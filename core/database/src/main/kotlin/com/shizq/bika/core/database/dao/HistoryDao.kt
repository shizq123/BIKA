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
}