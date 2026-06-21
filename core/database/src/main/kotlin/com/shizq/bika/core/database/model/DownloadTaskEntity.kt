package com.shizq.bika.core.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(
    tableName = "downloadTask",
    indices = [
        Index(value = ["status"]),
        Index(value = ["priority"]),
        Index(value = ["createdAt"]),
        Index(value = ["comicId", "episodeOrder"], unique = true),
    ],
)
data class DownloadTaskEntity(
    @PrimaryKey
    val id: String,

    val comicId: String,
    val comicTitle: String,
    val coverUrl: String,

    val episodeId: String,
    val episodeTitle: String,
    val episodeOrder: Int,

    val status: DownloadStatus = DownloadStatus.PENDING,

    /** 0~100 */
    val progress: Int = 0,
    val totalPages: Int = 0,
    val downloadedPages: Int = 0,

    /** 完成后对应本地目录 */
    val localPath: String = "",

    /** 是否本地查看过 */
    val isViewed: Boolean = false,

    /** 越大越靠前 */
    val priority: Int = 0,

    /** 失败原因码 */
    val errorCode: DownloadErrorCode = DownloadErrorCode.NONE,

    /** 失败详情文案 */
    val errorMessage: String = "",

    /** 已失败重试次数 */
    val retryCount: Int = 0,

    /** 创建时间 */
    val createdAt: Instant,

    /** 最后一次状态/进度更新时间 */
    val updatedAt: Instant,

    /** 完成时间 */
    val completedAt: Instant? = null,
)