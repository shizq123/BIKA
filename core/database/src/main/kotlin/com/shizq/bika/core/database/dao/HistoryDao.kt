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
     * 插入或更新一条完整的历史记录（包括已读章节）。
     * 必须使用 @Transaction 来确保两个插入操作要么都成功，要么都失败。
     */
    @Transaction
    suspend fun upsertHistory(history: HistoryRecordEntity, readChapters: List<ReadChapterEntity>) {
        upsertHistory(history) // 调用下面的辅助方法插入或更新主记录
        // 注意：这里可能需要先删除旧的 readChapters 记录，再插入新的
        // 或者根据业务逻辑进行更复杂的更新
        insertReadChapters(readChapters)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: HistoryRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadChapters(readChapters: List<ReadChapterEntity>)

    /**
     * 查询所有历史记录及其关联的已读章节。
     * 返回的是关系数据流。
     */
    @Transaction
    @Query("SELECT * FROM history ORDER BY lastReadAt DESC")
    fun getHistoriesWithReadChapters(): Flow<List<HistoryWithReadChapters>>
}