@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.comicinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.repository.ChapterRepository
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.scheduler.DownloadScheduler
import com.shizq.bika.ui.comicinfo.statemachine.UnitedDetailsStateMachine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * 漫画详情 ViewModel：详情、推荐、章节、下载、阅读进度。
 *
 * 评论域已拆到 [com.shizq.bika.ui.comicinfo.comments.CommentsViewModel]：
 * 它与这里没有共享状态，留在同一个类里只是让两组无关的依赖互相牵连。
 */
@HiltViewModel(assistedFactory = ComicInfoViewModel.Factory::class)
class ComicInfoViewModel @AssistedInject constructor(
    private val chapterRepository: ChapterRepository,
    stateMachineFactory: UnitedDetailsStateMachine.Factory,
    private val downloadTaskRepository: DownloadTaskRepository,
    private val downloadScheduler: DownloadScheduler,
    private val historyDao: ReadingHistoryDao,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    @Assisted private val comicId: String,
) : ViewModel() {

    private val stateMachine = stateMachineFactory.create(comicId).launchIn(viewModelScope)

    val state = stateMachine.state

    fun addBlockedTag(tag: String) {
        viewModelScope.launch {
            userPreferencesDataSource.addBlockedTag(tag)
        }
    }

    val downloadTasks = downloadTaskRepository.observeTasksByComic(comicId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val chapterProgress = historyDao.getChapterProgressByComic(comicId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * 章节列表。
     *
     * 分页与页尺寸都由 [ChapterRepository] 决定：原先这里自建 Pager + EpisodePagingSource，
     * 与 repository 的 getChapterList 打同一个端点做同一件事，两份实现已经分叉。
     */
    val episodesFlow: Flow<PagingData<Chapter>> = chapterRepository.getChapterList(comicId)
        .cachedIn(viewModelScope)

    fun dispatch(action: UnitedDetailsAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }

    /**
     * 获取漫画所有章节（用于 EpisodesPage 下载选择面板）。
     *
     * 翻页规则、页数上限、空页终止都在 [ChapterRepository.getAllChapters] 里，
     * 与章节目录共用同一份实现。异常直接抛给调用方，由 UI 的 catch 提示重试。
     */
    suspend fun fetchAllEpisodes(): List<Chapter> = chapterRepository.getAllChapters(comicId)

    /**
     * 将漫画所有章节加入下载队列，返回成功加入的数量。
     */
    suspend fun downloadAllEpisodes(comicTitle: String, coverUrl: String): Int {
        val allEps = fetchAllEpisodes()
        if (allEps.isEmpty()) return 0
        enqueueEpisodes(comicTitle, coverUrl, allEps)
        return allEps.size
    }

    /**
     * 将指定的漫画章节列表加入下载队列。
     */
    fun downloadEpisodes(comicTitle: String, coverUrl: String, episodes: List<Chapter>) {
        if (episodes.isEmpty()) return
        viewModelScope.launch {
            enqueueEpisodes(comicTitle, coverUrl, episodes)
        }
    }

    private suspend fun enqueueEpisodes(
        comicTitle: String,
        coverUrl: String,
        episodes: List<Chapter>,
    ) {
        val now = Clock.System.now()
        episodes.forEach { episode ->
            val taskId = taskId(comicId, episode.order)
            val task = DownloadTask(
                id = taskId,
                comicId = comicId,
                comicTitle = comicTitle,
                coverUrl = coverUrl,
                episodeId = episode.id,
                episodeTitle = episode.title,
                episodeOrder = episode.order,
                createdAt = now,
                updatedAt = now,
            )
            downloadTaskRepository.saveTask(task)
            downloadScheduler.enqueue(taskId)
        }
    }

    companion object {
        /** 生成任务 ID，与旧 DownloadRepository.taskId 保持一致 */
        fun taskId(comicId: String, episodeOrder: Int) = "${comicId}_$episodeOrder"
    }

    @AssistedFactory
    interface Factory {
        fun create(
            comicId: String,
        ): ComicInfoViewModel
    }
}
