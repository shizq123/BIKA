package com.shizq.bika.ui.download

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.data.repository.DownloadRepository
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.download.domain.DeleteDownloadTaskUseCase
import com.shizq.bika.core.download.domain.MoveDownloadTaskToFrontUseCase
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.scheduler.DownloadScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    val task: DownloadTask,
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
    private val downloadTaskRepository: DownloadTaskRepository,
    private val deleteDownloadTaskUseCase: DeleteDownloadTaskUseCase,
    private val moveToFrontUseCase: MoveDownloadTaskToFrontUseCase,
    private val downloadScheduler: DownloadScheduler,
    // 阅读进度属于 reading-history 领域，保持从 DownloadRepository 读取
    private val downloadRepository: DownloadRepository,
    private val historyDao: ReadingHistoryDao,
) : ViewModel() {

    /** 原始下载任务列表 */
    val tasks = downloadTaskRepository.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 全局所有漫画的阅读统计 Map（comicId → ComicReadSummary），
     * 供第一层漫画分组卡片实时显示已读完/阅读中章节数。
     */
    val allComicReadSummary: StateFlow<Map<String, ComicReadSummary>> =
        downloadRepository.getAllChapterProgress()
            .map { allProgress ->
                allProgress.groupBy { it.historyId }.mapValues { (_, progressList) ->
                    val finished = progressList.count { it.pageCount > 0 && it.currentPage >= it.pageCount }
                    val reading = progressList.count { it.pageCount > 0 && it.currentPage < it.pageCount }
                    ComicReadSummary(finishedCount = finished, readingCount = reading)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ---- 第二层：选中漫画的章节阅读进度 ----

    private val _selectedComicId = MutableStateFlow<String?>(null)

    /** 当前选中漫画的章节阅读进度 Map（chapterId -> ChapterProgressEntity） */
    val chapterProgressMap: StateFlow<Map<Int, ChapterProgressEntity>> =
        _selectedComicId.flatMapLatest { comicId ->
            if (comicId != null) {
                downloadRepository.getChapterProgressByComic(comicId)
                    .map { list -> list.associateBy { it.chapterId } }
            } else {
                flowOf(emptyMap())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectComic(comicId: String?) {
        _selectedComicId.value = comicId
    }

    fun bringToTop(task: DownloadTask) {
        viewModelScope.launch {
            moveToFrontUseCase(task.id)
        }
    }

    fun updateTaskPriority(task: DownloadTask, priority: Int) {
        viewModelScope.launch {
            downloadTaskRepository.updatePriority(task.id, priority)
        }
    }

    fun deleteDownload(task: DownloadTask) {
        viewModelScope.launch {
            deleteDownloadTaskUseCase(task.id)
        }
    }

    fun importCbz(uri: Uri, fileName: String) {
        downloadRepository.importCbzAsync(uri, fileName)
    }

    fun exportToCbz(task: DownloadTask) {
        // 文件导出操作委托给 DownloadRepository，由其负责本地路径解析与 CBZ 打包
        viewModelScope.launch {
            downloadRepository.exportToCbzByTask(task)
        }
    }

    fun exportMultipleToZip(tasks: List<DownloadTask>, comicTitle: String) {
        viewModelScope.launch {
            downloadRepository.exportMultipleToZipByTasks(tasks, comicTitle)
        }
    }

    fun deleteMultipleDownloads(tasks: List<DownloadTask>) {
        viewModelScope.launch {
            tasks.forEach { deleteDownloadTaskUseCase(it.id) }
        }
    }

    /**
     * 重试失败的下载任务。
     * 通过 core:download 的 Scheduler 重置状态并重新入队，
     * UI 层无需持有 Context。
     */
    fun retryDownload(task: DownloadTask) {
        viewModelScope.launch {
            // saveTask 保证任务记录存在（失败任务本身已在库里，这里幂等写入不影响）
            downloadTaskRepository.saveTask(task)
            downloadScheduler.resume(task.id)
        }
    }
}
