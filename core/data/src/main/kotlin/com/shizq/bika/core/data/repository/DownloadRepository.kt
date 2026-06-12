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
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.time.Clock
import com.shizq.bika.core.coroutine.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadTaskDao: DownloadTaskDao,
    private val network: BikaDataSource,
    private val okHttpClient: OkHttpClient,
    private val userPreferencesDataSource: com.shizq.bika.core.datastore.UserPreferencesDataSource,
    @ApplicationScope private val scope: CoroutineScope,
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

            val pendingPages = allPages.mapIndexed { index, imageUrl ->
                index to imageUrl
            }.filter { (index, _) ->
                val file = File(dir, "${String.format("%03d", index + 1)}.jpg")
                !file.exists()
            }

            val existsCount = totalPages - pendingPages.size
            val completedPages = AtomicInteger(existsCount)

            downloadTaskDao.updateProgress(
                taskId = taskId,
                downloadedPages = existsCount,
                totalPages = totalPages,
                progress = (existsCount * 100 / totalPages),
                status = DownloadStatus.DOWNLOADING,
            )

            if (pendingPages.isNotEmpty()) {
                val semaphore = Semaphore(5)
                coroutineScope {
                    pendingPages.map { (index, imageUrl) ->
                        async {
                            semaphore.withPermit {
                                val file = File(dir, "${String.format("%03d", index + 1)}.jpg")
                                downloadFile(imageUrl, file)
                                val currentCompleted = completedPages.incrementAndGet()
                                downloadTaskDao.updateProgress(
                                    taskId = taskId,
                                    downloadedPages = currentCompleted,
                                    totalPages = totalPages,
                                    progress = (currentCompleted * 100 / totalPages),
                                    status = DownloadStatus.DOWNLOADING,
                                )
                            }
                        }
                    }.awaitAll()
                }
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

    /** 批量删除下载任务及其本地文件 */
    suspend fun deleteDownloads(tasks: List<DownloadTaskEntity>) = withContext(Dispatchers.IO) {
        tasks.forEach { task ->
            if (task.localPath.isNotEmpty()) {
                File(task.localPath).deleteRecursively()
            }
        }
        downloadTaskDao.deleteTasks(tasks)
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
            if (!response.isSuccessful) {
                throw java.io.IOException("Unexpected HTTP code $response")
            }
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

    /** 导入本地 CBZ/ZIP 漫画 */
    suspend fun importCbz(uri: Uri, fileName: String) = withContext(Dispatchers.IO) {
        val cleanName = fileName.substringBeforeLast(".")
        val comicId = "local_import_${cleanName.hashCode().let { if (it < 0) -it else it }}"
        val episodeOrder = 1
        val taskId = taskId(comicId, episodeOrder)
        val dir = getEpisodeDir(comicId, episodeOrder)

        // 清理旧的导入目录，创建新目录
        dir.deleteRecursively()
        dir.mkdirs()

        // 将 Uri 对应的内容保存到临时缓存文件中
        val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}.zip")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("无法打开选择的文件数据流")

            val imageEntries = mutableListOf<ZipEntry>()
            ZipFile(tempFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (!entry.isDirectory && isImageFile(entry.name)) {
                        imageEntries.add(entry)
                    }
                }

                if (imageEntries.isEmpty()) {
                    throw Exception("压缩包中未找到任何有效的图片文件(jpg/jpeg/png/webp)")
                }

                // 统一按文件名排序，以便页码连续
                imageEntries.sortBy { it.name.lowercase() }

                // 提取并重命名为 001.jpg, 002.jpg ...
                imageEntries.forEachIndexed { index, entry ->
                    val destFile = File(dir, "${String.format("%03d", index + 1)}.jpg")
                    zip.getInputStream(entry).use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }

            // 注册到本地下载数据库
            val firstPage = File(dir, "001.jpg")
            val coverUrl = if (firstPage.exists()) Uri.fromFile(firstPage).toString() else ""
            val task = DownloadTaskEntity(
                id = taskId,
                comicId = comicId,
                comicTitle = cleanName,
                coverUrl = coverUrl,
                episodeId = UUID.randomUUID().toString(),
                episodeTitle = "本地导入",
                episodeOrder = episodeOrder,
                status = DownloadStatus.COMPLETED,
                progress = 100,
                totalPages = imageEntries.size,
                downloadedPages = imageEntries.size,
                localPath = dir.absolutePath,
                createdAt = Clock.System.now(),
                completedAt = Clock.System.now(),
            )
            downloadTaskDao.upsertTask(task)

        } finally {
            tempFile.delete()
        }
    }

    /** 导出已下载章节为本地 CBZ 文件 */
    suspend fun exportToCbz(task: DownloadTaskEntity): File = withContext(Dispatchers.IO) {
        val sourceDir = File(task.localPath)
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            throw Exception("找不到本地下载目录")
        }

        val exportDir = File(context.cacheDir, "exported_comics")
        exportDir.mkdirs()

        // 移除非法字符，生成干净的文件名
        val sanitizedTitle = task.comicTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val sanitizedEpisode = task.episodeTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val outputFile = File(exportDir, "$sanitizedTitle - $sanitizedEpisode.cbz")

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            sourceDir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension == "jpg") {
                    val entry = ZipEntry(file.name)
                    zos.putNextEntry(entry)
                    file.inputStream().use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
        outputFile
    }

    /** 批量导出已下载章节为单个 ZIP 文件 */
    suspend fun exportMultipleToZip(tasks: List<DownloadTaskEntity>, comicTitle: String): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exported_comics")
        exportDir.mkdirs()

        // 移除非法字符，生成干净的文件名
        val sanitizedComicTitle = comicTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val outputFile = File(exportDir, "${sanitizedComicTitle}_归档.zip")

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            tasks.forEach { task ->
                val sourceDir = File(task.localPath)
                if (sourceDir.exists() && sourceDir.isDirectory) {
                    val chapterFolderName = task.episodeTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    sourceDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.extension == "jpg") {
                            val entry = ZipEntry("$chapterFolderName/${file.name}")
                            zos.putNextEntry(entry)
                            file.inputStream().use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
        }
        outputFile
    }

    private fun isImageFile(name: String): Boolean {
        val ext = name.substringAfterLast(".", "").lowercase()
        return ext in listOf("jpg", "jpeg", "png", "webp")
    }

    private suspend fun showToast(message: String, duration: Int = android.widget.Toast.LENGTH_SHORT) {
        withContext(Dispatchers.Main) {
            android.widget.Toast.makeText(context, message, duration).show()
        }
    }

    private fun shareFile(file: File, mimeType: String, title: String) {
        try {
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                type = mimeType
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooserIntent = android.content.Intent.createChooser(shareIntent, title).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "分享文件失败", e)
            scope.launch {
                showToast("分享失败: ${e.localizedMessage}")
            }
        }
    }

    fun importCbzAsync(uri: Uri, fileName: String) {
        scope.launch {
            showToast("已在后台开始导入: $fileName")
            try {
                importCbz(uri, fileName)
                showToast("导入成功: $fileName")
            } catch (e: Exception) {
                Log.e(TAG, "导入失败: $fileName", e)
                showToast("导入失败: ${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    fun exportToCbzAsync(task: DownloadTaskEntity) {
        scope.launch {
            showToast("已在后台开始打包: ${task.episodeTitle}")
            try {
                val file = exportToCbz(task)
                showToast("打包成功: ${task.episodeTitle}")
                shareFile(file, "application/x-cbz", "导出为 CBZ")
            } catch (e: Exception) {
                Log.e(TAG, "导出失败", e)
                showToast("打包失败: ${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    fun exportMultipleToZipAsync(tasks: List<DownloadTaskEntity>, comicTitle: String) {
        scope.launch {
            showToast("已在后台开始打包 ${tasks.size} 个章节...")
            try {
                val file = exportMultipleToZip(tasks, comicTitle)
                showToast("打包成功: ${comicTitle}_归档")
                shareFile(file, "application/zip", "批量打包导出")
            } catch (e: Exception) {
                Log.e(TAG, "打包失败", e)
                showToast("打包失败: ${e.localizedMessage ?: "未知错误"}")
            }
        }
    }
}
