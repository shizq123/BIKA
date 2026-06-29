package com.shizq.bika.core.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.database.model.DownloadTaskEntity
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.collections.map
import kotlin.time.Clock

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val readingHistoryDao: ReadingHistoryDao,
    private val downloadTaskRepository: DownloadTaskRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    companion object {
        private const val TAG = "DownloadRepository"
    }

    // ---- 阅读进度（Reading History 领域） ----

    /** 获取所有漫画所有章节的阅读进度，实时 Flow */
    fun getAllChapterProgress(): Flow<List<ChapterProgressEntity>> =
        readingHistoryDao.getAllChapterProgress()

    /** 获取指定漫画的章节阅读进度，实时 Flow */
    fun getChapterProgressByComic(comicId: String): Flow<List<ChapterProgressEntity>> =
        readingHistoryDao.getChapterProgressByComic(comicId)

    // ---- 本地文件访问 ----

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

    // ---- CBZ 导入 ----

    /** 导入本地 CBZ/ZIP 漫画（异步，带 Toast 通知） */
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

    /** 导入本地 CBZ/ZIP 漫画（挂起，供 importCbzAsync 内部调用） */
    private suspend fun importCbz(uri: Uri, fileName: String) = withContext(Dispatchers.IO) {
        val cleanName = fileName.substringBeforeLast(".")
        val comicId = "local_import_${cleanName.hashCode().let { if (it < 0) -it else it }}"
        val episodeOrder = 1
        val taskId = "${comicId}_$episodeOrder"
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

            // 注册到本地下载数据库（通过 DownloadTaskRepository 统一入口）
            val firstPage = File(dir, "001.jpg")
            val coverUrl = if (firstPage.exists()) Uri.fromFile(firstPage).toString() else ""
            val task = DownloadTask(
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
                updatedAt = Clock.System.now(),
            )
            downloadTaskRepository.saveTask(task)

        } finally {
            tempFile.delete()
        }
    }

    // ---- CBZ / ZIP 导出 ----

    /** 导出指定章节为 CBZ（异步，带 Toast + 系统分享） */
    fun exportToCbzByTask(task: DownloadTask) {
        scope.launch {
            showToast("已在后台开始打包: ${task.episodeTitle}")
            try {
                val file = exportToCbz(task.toEntity())
                showToast("打包成功: ${task.episodeTitle}")
                shareFile(file, "application/x-cbz", "导出为 CBZ")
            } catch (e: Exception) {
                Log.e(TAG, "导出失败", e)
                showToast("打包失败: ${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    /** 批量导出章节为单个 ZIP（异步，带 Toast + 系统分享） */
    fun exportMultipleToZipByTasks(
        tasks: List<DownloadTask>,
        comicTitle: String,
    ) {
        scope.launch {
            showToast("已在后台开始打包 ${tasks.size} 个章节...")
            try {
                val file = exportMultipleToZip(tasks.map { it.toEntity() }, comicTitle)
                showToast("打包成功: ${comicTitle}_归档")
                shareFile(file, "application/zip", "批量打包导出")
            } catch (e: Exception) {
                Log.e(TAG, "打包失败", e)
                showToast("打包失败: ${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    // ---- 内部实现 ----

    private suspend fun exportToCbz(task: DownloadTaskEntity): File = withContext(Dispatchers.IO) {
        val sourceDir = File(task.localPath)
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            throw Exception("找不到本地下载目录")
        }

        val exportDir = File(context.cacheDir, "exported_comics")
        exportDir.mkdirs()

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

    private suspend fun exportMultipleToZip(
        tasks: List<DownloadTaskEntity>,
        comicTitle: String,
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exported_comics")
        exportDir.mkdirs()

        val sanitizedComicTitle = comicTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val outputFile = File(exportDir, "${sanitizedComicTitle}_归档.zip")

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            tasks.forEach { task ->
                val sourceDir = File(task.localPath)
                if (sourceDir.exists() && sourceDir.isDirectory) {
                    val chapterFolderName =
                        task.episodeTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
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

    private suspend fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, duration).show()
        }
    }

    private fun shareFile(file: File, mimeType: String, title: String) {
        try {
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooserIntent = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "分享文件失败", e)
            scope.launch {
                showToast("分享失败: ${e.localizedMessage}")
            }
        }
    }
}

// ---- 映射扩展 ----

private fun DownloadTask.toEntity() = DownloadTaskEntity(
    id = id,
    comicId = comicId,
    comicTitle = comicTitle,
    coverUrl = coverUrl,
    episodeId = episodeId,
    episodeTitle = episodeTitle,
    episodeOrder = episodeOrder,
    status = com.shizq.bika.core.database.model.DownloadStatus.valueOf(status.name),
    progress = progress,
    totalPages = totalPages,
    downloadedPages = downloadedPages,
    localPath = localPath,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
)
