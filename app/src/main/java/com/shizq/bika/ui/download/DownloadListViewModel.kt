package com.shizq.bika.ui.download

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.data.repository.DownloadRepository
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.download.domain.DeleteDownloadTaskUseCase
import com.shizq.bika.core.download.domain.MoveDownloadTaskToFrontUseCase
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.scheduler.DownloadScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DownloadListViewModel @Inject constructor(
    private val downloadTaskRepository: DownloadTaskRepository,
    private val deleteDownloadTaskUseCase: DeleteDownloadTaskUseCase,
    private val moveToFrontUseCase: MoveDownloadTaskToFrontUseCase,
    private val downloadScheduler: DownloadScheduler,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {
    // ---- 导航状态（单一数据源） ----
    private val _navState = MutableStateFlow<DownloadNavState>(DownloadNavState.ComicList)
    val navState: StateFlow<DownloadNavState> = _navState
    private val selectedComicId: StateFlow<String?> =
        navState
            .map { state -> (state as? DownloadNavState.ComicDetail)?.comicId }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun navigateToDetail(comicId: String) {
        _navState.value = DownloadNavState.ComicDetail(comicId)
    }

    fun navigateBack() {
        _navState.value = DownloadNavState.ComicList
    }

    // ---- 原始任务流 ----
    private val tasks = downloadTaskRepository.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // ---- 全局阅读统计 ----
    /**
     * 全局所有漫画的阅读统计 Map（comicId -> ComicReadSummary）
     * historyId 在当前业务上等同于 comicId
     */
    private val allComicReadSummary: StateFlow<Map<String, ComicReadSummary>> =
        downloadRepository.getAllChapterProgress()
            .map { allProgress ->
                allProgress
                    .groupBy { it.historyId }
                    .mapValues { (_, progressList) ->
                        val finished =
                            progressList.count { it.pageCount > 0 && it.currentPage >= it.pageCount }
                        val reading =
                            progressList.count { it.pageCount > 0 && it.currentPage < it.pageCount }
                        ComicReadSummary(
                            finishedCount = finished,
                            readingCount = reading,
                        )
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ---- 一级：漫画分组列表 ----
    val groupedComics: StateFlow<List<ComicDownloadGroup>> =
        combine(tasks, allComicReadSummary) { taskList, readSummaryMap ->
            taskList
                .groupBy { it.comicId }
                .map { (comicId, comicTasks) ->
                    val sortedTasks = comicTasks.sortedBy { it.episodeOrder }
                    val first = sortedTasks.first()
                    ComicDownloadGroup(
                        comicId = comicId,
                        comicTitle = first.comicTitle,
                        coverUrl = first.coverUrl,
                        tasks = sortedTasks,
                        readSummary = readSummaryMap[comicId],
                    )
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // ---- 二级：当前选中漫画的章节阅读进度 ----
    /**
     * 当前选中漫画的章节阅读进度 Map（episodeOrder -> ChapterProgressEntity）
     * chapterId 写入时即为 chapter.order，与 DownloadTask.episodeOrder 语义一致
     */
    private val chapterProgressMap: StateFlow<Map<Int, ChapterProgressEntity>> =
        selectedComicId.flatMapLatest { comicId ->
            if (comicId != null) {
                downloadRepository.getChapterProgressByComic(comicId)
                    .map { list -> list.associateBy { it.chapterId } }
            } else {
                flowOf(emptyMap())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * 当前选中漫画的章节列表（含阅读进度）
     */
    val selectedComicEpisodes: StateFlow<List<DownloadTaskWithProgress>> =
        combine(tasks, chapterProgressMap, selectedComicId) { taskList, progressMap, comicId ->
            if (comicId == null) return@combine emptyList()
            taskList
                .filter { it.comicId == comicId }
                .sortedBy { it.episodeOrder }
                .map { task ->
                    DownloadTaskWithProgress(
                        task = task,
                        readProgress = progressMap[task.episodeOrder],
                    )
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // 如果当前详情页对应漫画的任务被删空，自动返回一级列表
        viewModelScope.launch {
            combine(tasks, selectedComicId) { taskList, comicId ->
                comicId to taskList
            }.collect { (comicId, taskList) ->
                if (comicId != null && taskList.none { it.comicId == comicId }) {
                    navigateBack()
                }
            }
        }
    }

    // ---- 操作 ----
    fun bringToTop(task: DownloadTask) {
        viewModelScope.launch {
            moveToFrontUseCase(task.id)
        }
    }

    fun deleteDownload(task: DownloadTask) {
        viewModelScope.launch {
            deleteDownloadTaskUseCase(task.id)
        }
    }

    fun deleteMultipleDownloads(tasks: List<DownloadTask>) {
        viewModelScope.launch {
            coroutineScope {
                tasks.map { task ->
                    async { deleteDownloadTaskUseCase(task.id) }
                }.forEach { it.await() }
            }
        }
    }

    fun retryDownload(task: DownloadTask) {
        viewModelScope.launch {
            downloadTaskRepository.saveTask(task)
            downloadScheduler.resume(task.id)
        }
    }

    fun importCbz(uri: Uri, fileName: String) {
        downloadRepository.importCbzAsync(uri, fileName)
    }

    fun exportToCbz(task: DownloadTask) {
        viewModelScope.launch {
            downloadRepository.exportToCbzByTask(task)
        }
    }

    fun exportMultipleToZip(tasks: List<DownloadTask>, comicTitle: String) {
        viewModelScope.launch {
            downloadRepository.exportMultipleToZipByTasks(tasks, comicTitle)
        }
    }
}

/**
 * 下载章节 + 阅读进度的联合数据类，供 UI 展示阅读记录标记。
 */
data class DownloadTaskWithProgress(
    val task: DownloadTask,
    /** 该章节的阅读进度，null 表示从未阅读过 */
    val readProgress: ChapterProgressEntity? = null
) {
    val isRead: Boolean get() = readProgress != null

    /** 是否已读完该章节（当前页 >= 总页数） */
    val isFinished: Boolean
        get() = readProgress?.let { it.pageCount > 0 && it.currentPage >= it.pageCount } ?: false
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

/**
 * 一级列表使用的数据模型
 */
data class ComicDownloadGroup(
    val comicId: String,
    val comicTitle: String,
    val coverUrl: String,
    val tasks: List<DownloadTask>,
    val readSummary: ComicReadSummary? = null,
)