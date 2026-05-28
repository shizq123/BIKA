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
import kotlinx.coroutines.flow.combine
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
     * 下载任务 + 阅读记录联合 StateFlow。
     * 当进入某本漫画的详情页时（comicId 不为空），实时合并该漫画的章节阅读进度。
     * 当在漫画列表层（comicId 为空）时，仅返回空 Map，减少不必要的数据库开销。
     */
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

    fun deleteDownload(task: DownloadTaskEntity) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(task)
        }
    }
}
