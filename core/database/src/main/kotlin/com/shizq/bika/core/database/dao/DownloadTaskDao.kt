package com.shizq.bika.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.database.model.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

@Dao
interface DownloadTaskDao {

    @Upsert
    suspend fun upsertTask(task: DownloadTaskEntity)

    @Delete
    suspend fun deleteTask(task: DownloadTaskEntity)

    @Delete
    suspend fun deleteTasks(tasks: List<DownloadTaskEntity>)

    @Query("DELETE FROM downloadTask WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    /** 更新下载进度 */
    @Query(
        "UPDATE downloadTask SET downloadedPages = :downloadedPages, " +
            "totalPages = :totalPages, progress = :progress, status = :status " +
            "WHERE id = :taskId"
    )
    suspend fun updateProgress(
        taskId: String,
        downloadedPages: Int,
        totalPages: Int,
        progress: Int,
        status: DownloadStatus,
    )

    /** 更新下载状态（完成/失败） */
    @Query(
        "UPDATE downloadTask SET status = :status, completedAt = :completedAt, " +
            "localPath = :localPath WHERE id = :taskId"
    )
    suspend fun updateCompletion(
        taskId: String,
        status: DownloadStatus,
        completedAt: Instant?,
        localPath: String,
    )

    /** 标记为已本地查看 */
    @Query("UPDATE downloadTask SET isViewed = 1 WHERE id = :taskId")
    suspend fun markAsViewed(taskId: String)

    /** 获取所有下载任务，先按优先级降序，再按创建时间降序，供下载列表页使用 */
    @Query("SELECT * FROM downloadTask ORDER BY priority DESC, createdAt DESC")
    fun getAllTasks(): Flow<List<DownloadTaskEntity>>

    /** 获取某本漫画的所有已下载章节 */
    @Query("SELECT * FROM downloadTask WHERE comicId = :comicId ORDER BY episodeOrder ASC")
    fun getTasksByComic(comicId: String): Flow<List<DownloadTaskEntity>>

    /** 查询单个任务（用于判断某章节是否已下载） */
    @Query("SELECT * FROM downloadTask WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<DownloadTaskEntity?>

    /** 获取所有进行中的任务（App 重启后可恢复） */
    @Query("SELECT * FROM downloadTask WHERE status = 'DOWNLOADING' OR status = 'PENDING' ORDER BY priority DESC, createdAt ASC")
    suspend fun getPendingTasks(): List<DownloadTaskEntity>

    /** 更新任务优先级 */
    @Query("UPDATE downloadTask SET priority = :priority WHERE id = :taskId")
    suspend fun updateTaskPriority(taskId: String, priority: Int)

    /** 获取当前最大的 priority */
    @Query("SELECT IFNULL(MAX(priority), 0) FROM downloadTask")
    suspend fun getMaxPriority(): Int
}
