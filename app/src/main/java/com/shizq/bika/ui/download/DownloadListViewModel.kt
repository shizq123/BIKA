package com.shizq.bika.ui.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.data.repository.DownloadRepository
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.database.model.DownloadTaskEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 下载章节 + 阅读进度的联合数据类，供 UI 展示阅读记录标记。
 */
data class DownloadTaskWithProgress(
    val task: DownloadTaskEntity,
    /** 该章节的阅读进度，null 表示从未阅读过 */
    val readProgress: ChapterProgressEntity? = null
) {
    val isRead: Boolean get() = readProgress != null
    /** 是否已读完该章节（当前页 == 总页数） */
    val isFinished: Boolean
        get() = readProgress?.let { it.currentPage >= it.pageCount && it.pageCount > 0 } ?: false
}

/**
 * 单本漫画的阅读状态统计（供第一层卡片显示）
 */
data class ComicReadSummary(
    /** 已读完的章节数 */
    val finishedCount: Int,
    /** 阅读中（有进度但未读完）的章节数 */
    val readingCount: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DownloadListViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val readingHistoryDao: ReadingHistoryDao,
) : ViewModel() {

    /** 原始下载任务列表 */
    val tasks = downloadRepository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 全局所有漫画的阅读统计 Map（comicId → ComicReadSummary），
     * 供第一层漫画分组卡片实时显示已读完/阅读中章节数。
     */
    val allComicReadSummary: StateFlow<Map<String, ComicReadSummary>> =
        readingHistoryDao.getAllChapterProgress()
            .map { allProgress ->
                // 按 historyId（comicId）分组
                allProgress.groupBy { it.historyId }.mapValues { (_, progressList) ->
                    val finished = progressList.count { it.pageCount > 0 && it.currentPage >= it.pageCount }
                    val reading = progressList.count { it.pageCount > 0 && it.currentPage < it.pageCount }
                    ComicReadSummary(finishedCount = finished, readingCount = reading)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ---- 第二层：选中漫画的章节阅读进度 ----

    private val _selectedComicId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    /** 当前选中漫画的章节阅读进度 Map（chapterId -> ChapterProgressEntity） */
    val chapterProgressMap: StateFlow<Map<Int, ChapterProgressEntity>> =
        _selectedComicId.flatMapLatest { comicId ->
            if (comicId != null) {
                readingHistoryDao.getChapterProgressByComic(comicId)
                    .map { list -> list.associateBy { it.chapterId } }
            } else {
                flowOf(emptyMap())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectComic(comicId: String?) {
        _selectedComicId.value = comicId
    }

    fun bringToTop(task: DownloadTaskEntity) {
        viewModelScope.launch {
            downloadRepository.bringToTop(task.id)
        }
    }

    fun updateTaskPriority(task: DownloadTaskEntity, priority: Int) {
        viewModelScope.launch {
            downloadRepository.updateTaskPriority(task.id, priority)
        }
    }

    fun deleteDownload(task: DownloadTaskEntity) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(task)
        }
    }
}
