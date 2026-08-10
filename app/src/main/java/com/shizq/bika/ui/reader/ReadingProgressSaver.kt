package com.shizq.bika.ui.reader

import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.paging.ChapterMeta
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * 将阅读进度持久化到数据库。
 *
 * 所有写入均在 ApplicationScope 中执行，不随 ViewModel 销毁而取消，
 * 保证在退出阅读器、应用退到后台甚至进程被回收时，最后一次进度也能落库。
 */
class ReadingProgressSaver @Inject constructor(
    private val historyDao: ReadingHistoryDao,
    private val downloadTaskRepository: DownloadTaskRepository,
    @ApplicationScope private val externalScope: CoroutineScope,
) {
    fun save(comicId: String, chapterOrder: Int, meta: ChapterMeta?, pageIndex: Int) {
        if (comicId.isEmpty() || meta == null) return
        externalScope.launch(Dispatchers.IO) {
            val now = Clock.System.now()
            val affectedRows = historyDao.updateLastReadAt(comicId, now)
            if (affectedRows == 0) {
                // 历史条目不存在（如离线直接打开下载章节），为避免外键冲突，先插入默认漫画主历史记录
                val task = downloadTaskRepository.observeTask("${comicId}_${chapterOrder}").first()
                val title = task?.comicTitle ?: meta.title.ifEmpty { "Comic $comicId" }
                val coverUrl = task?.coverUrl ?: ""
                val newRecord = ReadingHistoryEntity(
                    id = comicId,
                    title = title,
                    author = "未知作者",
                    coverUrl = coverUrl,
                    lastInteractionAt = now
                )
                historyDao.upsertHistory(newRecord)
            }

            // 如果翻到最后一页或最后2页，则直接保存当前页为总页数，反馈已经看完
            val isFinished = meta.totalImages > 0 && pageIndex >= meta.totalImages - 2
            val savedPage = if (isFinished) meta.totalImages else pageIndex

            val chapterProgress = ChapterProgressEntity(
                historyId = comicId,
                chapterId = chapterOrder,
                currentPage = savedPage,
                pageCount = meta.totalImages,
                lastReadAt = now
            )
            historyDao.upsertChapterProgress(chapterProgress)

            // 如果看完，则同步将该章节的下载任务标记为已查看
            if (isFinished) {
                downloadTaskRepository.markAsViewed("${comicId}_${chapterOrder}")
            }
        }
    }
}
