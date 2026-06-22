package com.shizq.bika.core.download.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import okio.Source
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import kotlin.math.max

@Singleton
class DefaultLocalComicStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocalComicStorage {

    companion object {
        private const val ROOT_DIR_NAME = ".bika/comics"
        private const val TEMP_SUFFIX = ".download"

        private val SUPPORTED_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "avif", "heic"
        )

        private val MIME_TO_EXTENSION = mapOf(
            "image/jpeg" to "jpg",
            "image/jpg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
            "image/gif" to "gif",
            "image/bmp" to "bmp",
            "image/avif" to "avif",
            "image/heic" to "heic",
            "image/heif" to "heic",
        )
    }

    override fun resolveEpisodeDir(comicId: String, episodeOrder: Int): File {
        val rootDir = getRootDir()
        val comicDir = File(rootDir, sanitizePathSegment(comicId))
        return File(comicDir, episodeOrder.toString())
    }

    override fun prepareEpisodeDir(comicId: String, episodeOrder: Int): File {
        val rootDir = getRootDir()
        ensureDirectory(rootDir)
        val comicDir = File(rootDir, sanitizePathSegment(comicId))
        ensureDirectory(comicDir)
        val episodeDir = File(comicDir, episodeOrder.toString())
        ensureDirectory(episodeDir)
        cleanupStaleTempFiles(episodeDir)
        return episodeDir
    }

    override fun deleteEpisodeDir(dir: File): Boolean {
        if (!dir.exists()) return true
        val deleted = deleteRecursivelySafely(dir)
        if (deleted) {
            pruneEmptyAncestorDirs(
                start = dir.parentFile,
                stopBefore = getRootDir(),
            )
        }
        return deleted
    }

    private fun pruneEmptyAncestorDirs(
        start: File?,
        stopBefore: File,
    ) {
        var current = start
        while (current != null && current.exists() && current != stopBefore) {
            val children = current.listFiles()
            if (!children.isNullOrEmpty()) break
            if (!current.delete()) break
            current = current.parentFile
        }
    }

    override fun findExistingPageFile(dir: File, pageNumber: Int): File? {
        if (!dir.exists() || !dir.isDirectory) return null

        val pageStem = pageStem(pageNumber)

        val candidates = dir.listFiles()
            ?.asSequence()
            ?.filter { it.isFile }
            ?.filterNot { it.name.endsWith(TEMP_SUFFIX, ignoreCase = true) }
            ?.filter { it.nameWithoutExtension == pageStem }
            ?.filter { isSupportedImageFile(it.name) }
            ?.toList()
            .orEmpty()

        if (candidates.isEmpty()) return null

        // 零字节或损坏文件不算已完成文件，顺手清掉
        val validFiles = mutableListOf<File>()
        for (file in candidates) {
            if (file.length() > 0L) {
                validFiles += file
            } else {
                file.delete()
            }
        }

        if (validFiles.isEmpty()) return null

        // 正常情况下只应该有一个；如果历史遗留多个，取名字排序后的第一个
        // 其余保留不动，避免误删用户数据。后续导出阶段可再做收敛。
        return validFiles.sortedBy { it.name.lowercase(Locale.ROOT) }.first()
    }

    override fun buildPageFileName(pageNumber: Int, extension: String): String {
        val normalizedExt = normalizeExtension(extension)
        return "${pageStem(pageNumber)}.$normalizedExt"
    }

    override fun resolveImageExtension(url: String?, contentType: String?): String {
        val normalizedContentType = contentType
            ?.substringBefore(";")
            ?.trim()
            ?.lowercase(Locale.ROOT)

        val byMime = normalizedContentType?.let { MIME_TO_EXTENSION[it] }
        if (byMime != null) return byMime

        val byUrl = extractExtensionFromUrl(url)
        if (byUrl != null) return byUrl

        return "jpg"
    }

    override fun writePageAtomically(targetFile: File, source: Source) {
        val parent = targetFile.parentFile
            ?: throw IOException("目标文件没有父目录: ${targetFile.absolutePath}")

        ensureDirectory(parent)

        val tempFile = File(parent, targetFile.name + TEMP_SUFFIX)

        // 避免历史残留临时文件干扰
        if (tempFile.exists() && !tempFile.delete()) {
            throw IOException("无法删除旧临时文件: ${tempFile.absolutePath}")
        }

        try {
            FileOutputStream(tempFile).use { outputStream ->
                outputStream.sink().buffer().use { sink ->
                    sink.writeAll(source)
                    sink.flush()
                    outputStream.fd.sync()
                }
            }

            if (!tempFile.exists() || tempFile.length() <= 0L) {
                throw IOException("下载结果为空文件: ${tempFile.absolutePath}")
            }

            cleanupConflictingPageVariants(targetFile)

            if (targetFile.exists() && !targetFile.delete()) {
                throw IOException("无法替换旧目标文件: ${targetFile.absolutePath}")
            }

            if (!tempFile.renameTo(targetFile)) {
                throw IOException(
                    "原子重命名失败: ${tempFile.absolutePath} -> ${targetFile.absolutePath}"
                )
            }
        } catch (e: Throwable) {
            tempFile.delete()
            throw e
        }
    }

    override fun listPageFiles(dir: File): List<File> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val files = dir.listFiles() ?: return emptyList()

        return files
            .asSequence()
            .filter { it.isFile }
            .filterNot { it.name.endsWith(TEMP_SUFFIX, ignoreCase = true) }
            .filter { isSupportedImageFile(it.name) }
            .filter { it.length() > 0L }
            .sortedWith(
                compareBy<File> { extractPageNumber(it.name) ?: Int.MAX_VALUE }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
            .toList()
    }

    private fun getRootDir(): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(baseDir, ROOT_DIR_NAME)
    }

    private fun ensureDirectory(dir: File) {
        if (dir.exists()) {
            if (!dir.isDirectory) {
                throw IOException("目标路径不是目录: ${dir.absolutePath}")
            }
            return
        }

        if (!dir.mkdirs() && !dir.isDirectory) {
            throw IOException("无法创建目录: ${dir.absolutePath}")
        }
    }

    private fun cleanupStaleTempFiles(dir: File) {
        dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(TEMP_SUFFIX, ignoreCase = true) }
            ?.forEach { it.delete() }
    }

    private fun cleanupConflictingPageVariants(targetFile: File) {
        val parent = targetFile.parentFile ?: return
        val stem = targetFile.nameWithoutExtension

        parent.listFiles()
            ?.filter { it.isFile }
            ?.filterNot { it.name == targetFile.name }
            ?.filterNot { it.name.endsWith(TEMP_SUFFIX, ignoreCase = true) }
            ?.filter { it.nameWithoutExtension == stem }
            ?.forEach { it.delete() }
    }

    private fun deleteRecursivelySafely(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                if (!deleteRecursivelySafely(child)) {
                    return false
                }
            }
        }
        return !file.exists() || file.delete()
    }

    private fun pageStem(pageNumber: Int): String {
        val raw = pageNumber.coerceAtLeast(0).toString()
        val width = max(3, raw.length)
        return raw.padStart(width, '0')
    }

    private fun normalizeExtension(extension: String): String {
        val normalized = extension
            .trim()
            .trimStart('.')
            .lowercase(Locale.ROOT)

        return when (normalized) {
            "jpeg" -> "jpg"
            "heif" -> "heic"
            in SUPPORTED_EXTENSIONS -> normalized
            else -> "jpg"
        }
    }

    private fun extractExtensionFromUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null

        val lastSegment = runCatching { Uri.parse(url).lastPathSegment }.getOrNull()
            ?: return null

        val ext = lastSegment.substringAfterLast('.', missingDelimiterValue = "")
            .substringBefore('?')
            .substringBefore('#')
            .trim()
            .lowercase(Locale.ROOT)

        if (ext.isBlank()) return null

        val normalized = normalizeExtension(ext)
        return if (normalized in SUPPORTED_EXTENSIONS || normalized == "jpg") {
            normalized
        } else {
            null
        }
    }

    private fun isSupportedImageFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
        return ext in SUPPORTED_EXTENSIONS
    }

    private fun extractPageNumber(fileName: String): Int? {
        val stem = fileName.substringBeforeLast('.', missingDelimiterValue = fileName)
        return stem.toIntOrNull()
    }

    private fun sanitizePathSegment(value: String): String {
        val sanitized = value
            .trim()
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .replace(Regex("""\s+"""), " ")
            .trim('.')
            .trim()

        return sanitized.ifBlank { "_" }
    }
}