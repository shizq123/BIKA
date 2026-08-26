package com.shizq.bika.feature.reader.impl

import com.shizq.bika.core.common.BikaLog
import com.shizq.bika.core.data.paging.ChapterMeta
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * 将阅读进度持久化到数据库。
 *
 * [save] 采用**同步阻塞写库**（runBlocking + IO）：调用方线程（主线程）会短暂等待写库完成，
 * 保证返回时数据已落库。这消除了对 ApplicationScope/异步队列的依赖，
 * 即使返回/退后台后进程被立即回收，进度也不会丢失；任何写库异常也会当场捕获并提示。
 *
 * 常规翻页的自动保存应使用 [saveSuspend]（协程内异步写库，不阻塞主线程）；
 * 仅在返回、退后台、销毁等关键时机使用同步 [save]。
 */
@Singleton
class ReadingProgressStore @Inject constructor(
    private val historyDao: ReadingHistoryDao,
    private val downloadTaskRepository: DownloadTaskRepository,
) {
    /**
     * 同步阻塞写库（关键时机专用：返回/退后台/销毁）。返回时已落库。
     * @return 写库是否成功
     */
    fun save(comicId: String, chapterOrder: Int, meta: ChapterMeta?, pageIndex: Int): Boolean =
        runBlocking { saveSuspend(comicId, chapterOrder, meta, pageIndex) }

    /**
     * 协程内异步写库（常规翻页自动保存用），不阻塞调用线程。
     * @return 写库是否成功
     */
    suspend fun saveSuspend(comicId: String, chapterOrder: Int, meta: ChapterMeta?, pageIndex: Int): Boolean {
        if (comicId.isEmpty()) {
            BikaLog.w(TAG, "跳过保存: comicId 为空 章节=$chapterOrder 页=$pageIndex")
            return false
        }
        // meta 为 null 时（ChapterMetaLoaded 还未到达），用 totalImages=0 占位保存。
        // 恢复时 pageCount=0 → 条件 pageCount>0 为 false → 直接用 currentPage，不会误判为已看完。
        val effectiveMeta = meta ?: ChapterMeta(title = "", totalImages = 0)
        return try {
            withContext(Dispatchers.IO) {
                saveInternal(comicId, chapterOrder, effectiveMeta, pageIndex)
            }
            BikaLog.d(TAG, "进度已落库: comic=$comicId 章节=$chapterOrder 页=$pageIndex/${effectiveMeta.totalImages}")
            true
        } catch (e: Exception) {
            BikaLog.e(TAG, "进度落库失败: comic=$comicId 章节=$chapterOrder 页=$pageIndex", e)
            false
        }
    }

    private suspend fun saveInternal(comicId: String, chapterOrder: Int, meta: ChapterMeta, pageIndex: Int) {
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

        // 如果翻到最后 3 页（即 pageIndex >= totalImages - 3 或 totalImages <= 3 时），判定为看完了本章
        val isFinished = meta.totalImages > 0 && pageIndex >= (meta.totalImages - 3).coerceAtLeast(0)

        val chapterProgress = ChapterProgressEntity(
            historyId = comicId,
            chapterId = chapterOrder,
            currentPage = pageIndex,
            pageCount = meta.totalImages,
            lastReadAt = now
        )
        historyDao.upsertChapterProgress(chapterProgress)

        // 如果看完，则同步将该章节的下载任务标记为已查看
        if (isFinished) {
            downloadTaskRepository.markAsViewed("${comicId}_${chapterOrder}")
        }
    }

    private companion object {
        const val TAG = "ReaderProgress"
    }
}
