package com.shizq.bika.core.download.storage

import okio.Source
import java.io.File

interface LocalComicStorage {

    /** 获取并确保章节目录可用，不可用时抛异常 */
    fun prepareEpisodeDir(comicId: String, episodeOrder: Int): File

    /** 根据页码查找已存在的最终图片文件，需忽略临时文件 */
    fun findExistingPageFile(dir: File, pageNumber: Int): File?

    /** 构建最终页文件名，例如 001.jpg / 002.webp */
    fun buildPageFileName(pageNumber: Int, extension: String): String

    /** 根据 URL / Content-Type 解析图片扩展名 */
    fun resolveImageExtension(url: String?, contentType: String?): String

    /**
     * 原子写入：
     * 先写临时文件，再 rename 为最终文件
     * 若存在同页不同扩展名旧文件，可在实现里做清理
     */
    fun writePageAtomically(targetFile: File, source: Source)

    /** 获取本地章节所有图片文件，按页序排序 */
    fun listPageFiles(dir: File): List<File>
}