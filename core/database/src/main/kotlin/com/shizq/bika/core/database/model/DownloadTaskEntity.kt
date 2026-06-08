package com.shizq.bika.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

/**
 * 漫画下载任务实体，以章节（Episode）为单位存储。
 */
@Entity(tableName = "downloadTask")
data class DownloadTaskEntity(
    /** 任务 ID，格式：{comicId}_{episodeOrder} */
    @PrimaryKey
    val id: String,

    // ---- 漫画基本信息 ----
    val comicId: String,
    val comicTitle: String,
    val coverUrl: String,

    // ---- 章节信息 ----
    val episodeId: String,
    val episodeTitle: String,
    val episodeOrder: Int,

    // ---- 下载状态 ----
    /** 下载状态：PENDING / DOWNLOADING / COMPLETED / FAILED */
    val status: DownloadStatus = DownloadStatus.PENDING,

    /** 下载进度 0-100 */
    val progress: Int = 0,

    /** 总页数 */
    val totalPages: Int = 0,

    /** 已下载页数 */
    val downloadedPages: Int = 0,

    /** 本地存储路径（目录） */
    val localPath: String = "",

    /** 是否已在本地查看过 */
    val isViewed: Boolean = false,

    /** 任务创建时间 */
    val createdAt: Instant,

    /** 下载完成时间（未完成时为 null） */
    val completedAt: Instant? = null,

    /** 任务优先级，数值越大越优先 */
    val priority: Int = 0,
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
}
