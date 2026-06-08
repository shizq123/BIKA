package com.shizq.bika.core.data.repository

import android.content.Context
import android.util.Log
import com.shizq.bika.core.database.dao.DownloadTaskDao
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.database.model.DownloadTaskEntity
import com.shizq.bika.core.network.BikaDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import kotlin.time.Clock

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadTaskDao: DownloadTaskDao,
    private val network: BikaDataSource,
    private val okHttpClient: OkHttpClient,
    private val userPreferencesDataSource: com.shizq.bika.core.datastore.UserPreferencesDataSource,
) {
    companion object {
        private const val TAG = "DownloadRepository"

        /** 生成任务 ID */
        fun taskId(comicId: String, episodeOrder: Int) = "${comicId}_$episodeOrder"
    }

    /** 获取所有下载任务（供下载列表页使用） */
    fun getAllTasks(): Flow<List<DownloadTaskEntity>> = downloadTaskDao.getAllTasks()

    /** 获取某本漫画的下载任务 */
    fun getTasksByComic(comicId: String): Flow<List<DownloadTaskEntity>> =
        downloadTaskDao.getTasksByComic(comicId)

    /** 获取单个任务状态 */
    fun getTaskById(taskId: String): Flow<DownloadTaskEntity?> =
        downloadTaskDao.getTaskById(taskId)

    /**
     * 开始下载某个章节。
     * 此方法在 IO 协程中执行，调用方需自行提供协程上下文。
     */
    suspend fun downloadEpisode(
        comicId: String,
        comicTitle: String,
        coverUrl: String,
        episodeId: String,
        episodeTitle: String,
        episodeOrder: Int,
    ) = withContext(Dispatchers.IO) {
        val taskId = taskId(comicId, episodeOrder)

        // 创建或重置任务
        val task = DownloadTaskEntity(
            id = taskId,
            comicId = comicId,
            comicTitle = comicTitle,
            coverUrl = coverUrl,
            episodeId = episodeId,
            episodeTitle = episodeTitle,
            episodeOrder = episodeOrder,
            status = DownloadStatus.DOWNLOADING,
            createdAt = Clock.System.now(),
        )
        downloadTaskDao.upsertTask(task)

        try {
            // 离线下载 WiFi 环境检查
            val userPrefs = userPreferencesDataSource.userData.first()
            if (userPrefs.downloadOverWifiOnly && !isWifiConnected(context)) {
                throw Exception("已开启仅在WiFi下下载偏好，当前处于非WiFi（蜂窝移动）网络环境，已安全拦截下载并挂起任务")
            }

            // 获取该章节的所有页面图片 URL
            var page = 1
            val allPages = mutableListOf<String>()
            while (true) {
                val data = network.getChapterPages(comicId, episodeOrder, page)
                allPages.addAll(data.paginationData.images.map { it.media.originalImageUrl })
                if (page >= data.paginationData.totalPages) break
                page++
            }

            val totalPages = allPages.size
            val dir = getEpisodeDir(comicId, episodeOrder)
            dir.mkdirs()

            allPages.forEachIndexed { index, imageUrl ->
                val file = File(dir, "${String.format("%03d", index + 1)}.jpg")
                if (!file.exists()) {
                    downloadFile(imageUrl, file)
                }
                downloadTaskDao.updateProgress(
                    taskId = taskId,
                    downloadedPages = index + 1,
                    totalPages = totalPages,
                    progress = ((index + 1) * 100 / totalPages),
                    status = DownloadStatus.DOWNLOADING,
                )
            }

            downloadTaskDao.updateCompletion(
                taskId = taskId,
                status = DownloadStatus.COMPLETED,
                completedAt = Clock.System.now(),
                localPath = dir.absolutePath,
            )
            Log.i(TAG, "下载完成: $comicTitle - $episodeTitle")
        } catch (e: Exception) {
            Log.e(TAG, "下载失败: $comicTitle - $episodeTitle", e)
            downloadTaskDao.updateCompletion(
                taskId = taskId,
                status = DownloadStatus.FAILED,
                completedAt = null,
                localPath = "",
            )
        }
    }

    /** 标记章节为已在本地查看 */
    suspend fun markAsViewed(taskId: String) {
        downloadTaskDao.markAsViewed(taskId)
    }

    /** 置顶指定的下载任务 */
    suspend fun bringToTop(taskId: String) = withContext(Dispatchers.IO) {
        val maxPriority = downloadTaskDao.getMaxPriority()
        downloadTaskDao.updateTaskPriority(taskId, maxPriority + 1)
    }

    /** 更新指定的任务优先级 */
    suspend fun updateTaskPriority(taskId: String, priority: Int) = withContext(Dispatchers.IO) {
        downloadTaskDao.updateTaskPriority(taskId, priority)
    }

    /** 删除下载任务及其本地文件 */
    suspend fun deleteDownload(task: DownloadTaskEntity) = withContext(Dispatchers.IO) {
        if (task.localPath.isNotEmpty()) {
            File(task.localPath).deleteRecursively()
        }
        downloadTaskDao.deleteTask(task)
    }

    /** 获取隐藏目录下的章节文件夹 */
    fun getEpisodeDir(comicId: String, episodeOrder: Int): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, ".bika/comics/$comicId/$episodeOrder")
    }

    /** 获取已下载章节的本地图片文件列表（用于本地阅读） */
    fun getLocalImages(comicId: String, episodeOrder: Int): List<File> {
        val dir = getEpisodeDir(comicId, episodeOrder)
        return dir.listFiles()
            ?.filter { it.isFile && it.extension == "jpg" }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    private fun downloadFile(url: String, dest: File) {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body
            dest.sink().buffer().use { sink ->
                sink.writeAll(body.source())
            }
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }
}
