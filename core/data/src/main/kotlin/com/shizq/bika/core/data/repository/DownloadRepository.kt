package com.shizq.bika.core.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.data.platform.FileShareProvider
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.database.model.DownloadTaskEntity
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.storage.LocalComicStorage
import com.shizq.bika.core.message.MessageReporter
import com.shizq.bika.core.message.UiText
import com.shizq.bika.core.message.reportError
import com.shizq.bika.core.message.reportInfo
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
import kotlin.time.Clock

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val readingHistoryDao: ReadingHistoryDao,
    private val downloadTaskRepository: DownloadTaskRepository,
    private val localComicStorage: LocalComicStorage,
    private val fileShareProvider: FileShareProvider,
    private val messageReporter: MessageReporter,
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

    /**
     * 获取隐藏目录下的章节文件夹。
     *
     * 委托给 [LocalComicStorage.resolveEpisodeDir]（core:download 的唯一权威实现），
     * 不再自行拼路径：此前这里硬编码 `.bika/comics/$comicId/$episodeOrder` 且不做
     * sanitizePathSegment，与下载模块的目录布局各算各的，comicId 含路径分隔符等
     * 特殊字符时两侧解析出的目录会不一致。
     */
    fun getEpisodeDir(comicId: String, episodeOrder: Int): File =
        localComicStorage.resolveEpisodeDir(comicId, episodeOrder)

    /**
     * 获取已下载章节的本地图片文件列表（用于本地阅读）。
     *
     * 委托给 [LocalComicStorage.listPageFiles]：此前这里只认 `.jpg` 扩展名、
     * 按文件名字符串排序，而下载模块实际支持 jpg/jpeg/png/webp/gif/bmp/avif/heic
     * 并按解析出的页码数字排序。下载为非 jpg 格式的章节离线阅读会得到空列表或
     * 页面顺序错乱，现在统一到下载模块的实现，两侧不再可能出现不一致。
     */
    fun getLocalImages(comicId: String, episodeOrder: Int): List<File> =
        localComicStorage.listPageFiles(getEpisodeDir(comicId, episodeOrder))

    // ---- CBZ 导入 ----

    /** 导入本地 CBZ/ZIP 漫画（异步，带消息通知） */
    fun importCbzAsync(uri: Uri, fileName: String) {
        scope.launch {
            messageReporter.reportInfo(UiText.of("已在后台开始导入: $fileName"))
            try {
                importCbz(uri, fileName)
                messageReporter.reportInfo(UiText.of("导入成功: $fileName"))
            } catch (e: Exception) {
                Log.e(TAG, "导入失败: $fileName", e)
                messageReporter.reportError(
                    UiText.of("导入失败: ${e.localizedMessage ?: "未知错误"}"),
                )
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

    /** 导出指定章节为 CBZ（异步，带消息通知 + 系统分享） */
    fun exportToCbzByTask(task: DownloadTask) {
        scope.launch {
            messageReporter.reportInfo(UiText.of("已在后台开始打包: ${task.episodeTitle}"))
            try {
                val file = exportToCbz(task.toEntity())
                messageReporter.reportInfo(UiText.of("打包成功: ${task.episodeTitle}"))
                shareFile(file, "application/x-cbz", "导出为 CBZ")
            } catch (e: Exception) {
                Log.e(TAG, "导出失败", e)
                messageReporter.reportError(
                    UiText.of("打包失败: ${e.localizedMessage ?: "未知错误"}"),
                )
            }
        }
    }

    /** 批量导出章节为单个 ZIP（异步，带消息通知 + 系统分享） */
    fun exportMultipleToZipByTasks(
        tasks: List<DownloadTask>,
        comicTitle: String,
    ) {
        scope.launch {
            messageReporter.reportInfo(UiText.of("已在后台开始打包 ${tasks.size} 个章节..."))
            try {
                val file = exportMultipleToZip(tasks.map { it.toEntity() }, comicTitle)
                messageReporter.reportInfo(UiText.of("打包成功: ${comicTitle}_归档"))
                shareFile(file, "application/zip", "批量打包导出")
            } catch (e: Exception) {
                Log.e(TAG, "打包失败", e)
                messageReporter.reportError(
                    UiText.of("打包失败: ${e.localizedMessage ?: "未知错误"}"),
                )
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

        // 用 listPageFiles 而非本地硬编码的 extension == "jpg" 过滤：
        // 下载模块支持 jpg/jpeg/png/webp/gif/bmp/avif/heic，只认 jpg 会让非 jpg
        // 格式下载的章节导出为缺页的 CBZ。listPageFiles 同时保证了按页码排序。
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            localComicStorage.listPageFiles(sourceDir).forEach { file ->
                val entry = ZipEntry(file.name)
                zos.putNextEntry(entry)
                file.inputStream().use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
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
                    // 同上：统一用 listPageFiles，不再只打包 jpg
                    localComicStorage.listPageFiles(sourceDir).forEach { file ->
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
        outputFile
    }

    private fun isImageFile(name: String): Boolean {
        val ext = name.substringAfterLast(".", "").lowercase()
        return ext in listOf("jpg", "jpeg", "png", "webp")
    }

    private fun shareFile(file: File, mimeType: String, title: String) {
        try {
            fileShareProvider.share(file, mimeType, title)
        } catch (e: Exception) {
            Log.e(TAG, "分享文件失败", e)
            messageReporter.reportError(UiText.of("分享失败: ${e.localizedMessage}"))
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
    status = DownloadStatus.valueOf(status.name),
    progress = progress,
    totalPages = totalPages,
    downloadedPages = downloadedPages,
    localPath = localPath,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
)
