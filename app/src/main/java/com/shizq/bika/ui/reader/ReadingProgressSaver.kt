package com.shizq.bika.ui.reader

import com.shizq.bika.core.common.BikaLog
import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.paging.ChapterMeta
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * 将阅读进度持久化到数据库。
 *
 * 所有写入均在 ApplicationScope 中执行，不随 ViewModel 销毁而取消，
 * 保证在退出阅读器、应用退到后台甚至进程被回收时，最后一次进度也能落库。
 *
 * 保存请求通过 FIFO 队列由单一 worker 串行落库：
 * 自动保存（debounce）、返回键、ON_STOP、onDispose 等多路并发触发时，
 * 后提交的进度（如用户刚翻到的最新页）一定最后写入，避免旧值覆盖新值。
 * 单个请求落库失败只记录日志，不会中断后续请求。
 */
@Singleton
class ReadingProgressSaver @Inject constructor(
    private val historyDao: ReadingHistoryDao,
    private val downloadTaskRepository: DownloadTaskRepository,
    @ApplicationScope private val externalScope: CoroutineScope,
) {
    private data class SaveRequest(
        val comicId: String,
        val chapterOrder: Int,
        val meta: ChapterMeta,
        val pageIndex: Int,
    )

    private val queue = Channel<SaveRequest>(Channel.UNLIMITED)

    init {
        externalScope.launch(Dispatchers.IO) {
            for (request in queue) {
                try {
                    saveInternal(request)
                    BikaLog.d(
                        TAG,
                        "进度已落库: comic=${request.comicId} 章节=${request.chapterOrder} 页=${request.pageIndex}/${request.meta.totalImages}"
                    )
                } catch (e: Exception) {
                    // 单次失败必须被吞掉并记录，否则 worker 终止会导致后续所有进度静默丢失
                    BikaLog.e(
                        TAG,
                        "进度落库失败: comic=${request.comicId} 章节=${request.chapterOrder} 页=${request.pageIndex}",
                        e
                    )
                }
            }
        }
    }

    fun save(comicId: String, chapterOrder: Int, meta: ChapterMeta?, pageIndex: Int) {
        if (comicId.isEmpty() || meta == null) {
            BikaLog.w(TAG, "跳过保存: comicId='$comicId' 章节=$chapterOrder 页=$pageIndex meta=${meta == null}")
            return
        }
        BikaLog.d(TAG, "保存请求入队: comic=$comicId 章节=$chapterOrder 页=$pageIndex")
        queue.trySend(SaveRequest(comicId, chapterOrder, meta, pageIndex))
    }

    private suspend fun saveInternal(request: SaveRequest) {
        val now = Clock.System.now()
        val affectedRows = historyDao.updateLastReadAt(request.comicId, now)
        if (affectedRows == 0) {
            // 历史条目不存在（如离线直接打开下载章节），为避免外键冲突，先插入默认漫画主历史记录
            val task = downloadTaskRepository.observeTask("${request.comicId}_${request.chapterOrder}").first()
            val title = task?.comicTitle ?: request.meta.title.ifEmpty { "Comic ${request.comicId}" }
            val coverUrl = task?.coverUrl ?: ""
            val newRecord = ReadingHistoryEntity(
                id = request.comicId,
                title = title,
                author = "未知作者",
                coverUrl = coverUrl,
                lastInteractionAt = now
            )
            historyDao.upsertHistory(newRecord)
        }

        // 如果翻到最后一页或最后2页，则直接保存当前页为总页数，反馈已经看完
        val isFinished = request.meta.totalImages > 0 && request.pageIndex >= request.meta.totalImages - 2
        val savedPage = if (isFinished) request.meta.totalImages else request.pageIndex

        val chapterProgress = ChapterProgressEntity(
            historyId = request.comicId,
            chapterId = request.chapterOrder,
            currentPage = savedPage,
            pageCount = request.meta.totalImages,
            lastReadAt = now
        )
        historyDao.upsertChapterProgress(chapterProgress)

        // 如果看完，则同步将该章节的下载任务标记为已查看
        if (isFinished) {
            downloadTaskRepository.markAsViewed("${request.comicId}_${request.chapterOrder}")
        }
    }

    private companion object {
        const val TAG = "ReaderProgress"
    }
}
