package com.shizq.bika.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DetailedHistory
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingHistoryDao {

    /**
     * 插入或更新一条阅读历史记录。
     * 如果主键已存在，则会替换旧数据。
     * @param history ReadingHistoryEntity 对象
     */
    @Upsert
    suspend fun upsertHistory(history: ReadingHistoryEntity)

    /**
     * 插入或更新多条阅读历史记录。
     * @param histories ReadingHistoryEntity 列表
     */
    @Upsert
    suspend fun upsertHistories(histories: List<ReadingHistoryEntity>)

    /**
     * 删除一条阅读历史记录。
     * 由于在 ChapterProgressEntity 中设置了外键并指定了 onDelete = ForeignKey.CASCADE，
     * 删除 ReadingHistoryEntity 时，所有关联的 ChapterProgressEntity 也会被自动删除。
     * @param history 要删除的 ReadingHistoryEntity 对象
     */
    @Delete
    suspend fun deleteHistory(history: ReadingHistoryEntity)

    /**
     * 根据 ID 删除一条阅读历史记录。
     * @param id 历史记录的 ID
     */
    @Query("DELETE FROM reading_history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)

    /**
     * 清空所有阅读历史记录。
     * 同样，所有关联的章节进度也会被级联删除。
     */
    @Query("DELETE FROM reading_history")
    suspend fun clearAllHistories()

    /**
     * 插入或更新一个章节的阅读进度。
     * @param progress ChapterProgressEntity 对象
     */
    @Upsert
    suspend fun upsertChapterProgress(progress: ChapterProgressEntity)

    /**
     * 插入或更新多个章节的阅读进度。
     * @param progressList ChapterProgressEntity 列表
     */
    @Upsert
    suspend fun upsertChapterProgressList(progressList: List<ChapterProgressEntity>)

    /**
     * [核心功能] 获取所有详细的阅读历史记录，并包含它们各自的章节进度列表。
     * 使用 @Transaction 注解可以确保“查询历史记录”和“查询其关联的章节进度”这两个操作是原子性的。
     * 返回一个 Flow，当数据发生变化时，可以自动收到更新。
     * 结果按照最后交互时间降序排列，最新的历史记录会排在最前面。
     *
     * @return 返回一个包含 DetailedHistory 列表的 Flow
     */
    @Transaction
    @Query("SELECT * FROM reading_history ORDER BY lastInteractionAt DESC")
    fun getDetailedHistories(): Flow<List<DetailedHistory>>

    /**
     * [核心功能] 根据 ID 获取单个详细的阅读历史记录。
     * 同样使用 @Transaction 来保证数据查询的原子性。
     * @param id 历史记录的 ID
     * @return 返回单个 DetailedHistory 对象，可能为 null
     */
    @Transaction
    @Query("SELECT * FROM reading_history WHERE id = :id")
    suspend fun getDetailedHistoryById(id: String): DetailedHistory?

    /**
     * 根据 historyId 获取所有相关的章节进度。
     * @param historyId 关联的 ReadingHistoryEntity 的 ID
     * @return 返回 ChapterProgressEntity 列表
     */
    @Query("SELECT * FROM chapter_progress WHERE historyId = :historyId ORDER BY chapterIndex ASC")
    suspend fun getProgressListByHistoryId(historyId: String): List<ChapterProgressEntity>
}