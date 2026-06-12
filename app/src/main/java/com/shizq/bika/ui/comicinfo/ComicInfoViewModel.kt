@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.comicinfo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.core.network.model.Type
import com.shizq.bika.paging.EpisodePagingSource
import com.shizq.bika.ui.comicinfo.paging.CommentPagingSource
import com.shizq.bika.ui.comicinfo.paging.ReplyPagingSource
import com.shizq.bika.ui.comicinfo.statemachine.UnitedDetailsStateMachine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import com.shizq.bika.core.database.dao.DownloadTaskDao
import com.shizq.bika.core.database.model.DownloadTaskEntity
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import android.content.Context
import com.shizq.bika.sync.workers.DownloadWorker

private const val TAG = "ComicInfoViewModel"

@HiltViewModel(assistedFactory = ComicInfoViewModel.Factory::class)
class ComicInfoViewModel @AssistedInject constructor(
    private val network: BikaDataSource,
    private val commentPagingSourceFactory: CommentPagingSource.Factory,
    private val replyPagingSourceFactory: ReplyPagingSource.Factory,
    stateMachineFactory: UnitedDetailsStateMachine.Factory,
    private val downloadTaskDao: DownloadTaskDao,
    private val historyDao: ReadingHistoryDao,
    @Assisted private val id: String,
) : ViewModel() {

    private val stateMachine = stateMachineFactory.create(id).launchIn(viewModelScope)

    val state = stateMachine.state

    val downloadTasks = downloadTaskDao.getTasksByComic(id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val chapterProgress = historyDao.getChapterProgressByComic(id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val episodesFlow: Flow<PagingData<Episode>> = Pager(PagingConfig(40)) {
        EpisodePagingSource(network, id)
    }
        .flow
        .cachedIn(viewModelScope)
    val pinnedComments: StateFlow<List<Comment>>
        field = MutableStateFlow(emptyList())
    val regularComments: Flow<PagingData<Comment>> = Pager(PagingConfig(40)) {
        commentPagingSourceFactory(id) {
            pinnedComments.value = it
        }
    }.flow.cachedIn(viewModelScope)

    val replyList = state.map {
        (it as? UnitedDetailsUiState.Content)?.viewingRepliesForId
    }.filterNotNull()
        .flatMapLatest {
            Pager(PagingConfig(5)) {
                replyPagingSourceFactory(it)
            }.flow
        }.cachedIn(viewModelScope)

    fun dispatch(action: UnitedDetailsAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }
    fun toggleCommentLike(id: String) {
        viewModelScope.launch {
            try {
                network.toggleCommentLike(id)
            } catch (e: Exception) {
                Log.e(TAG, "toggleCommentLike: ", e)
            }
        }
    }

    fun postComment(text: String, replyToCommentId: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (replyToCommentId == null) {
                    network.addReply(Type.COMIC, id, text)
                } else {
                    network.addCommentReply(replyToCommentId, text)
                }
                onResult(true)
            } catch (e: Exception) {
                Log.e(TAG, "postComment failed", e)
                onResult(false)
            }
        }
    }

    /**
     * 获取漫画所有章节列表（用于 EpisodesPage 下载选择面板）。
     * 将网络调用封装在 ViewModel，避免 Composable 直接访问网络层。
     */
    suspend fun fetchAllEpisodes(): List<Episode> {
        val list = mutableListOf<Episode>()
        var pageIndex = 1
        var hasNext = true
        while (hasNext) {
            val res = network.getComicEpisodes(id, pageIndex)
            list.addAll(res.eps.docs)
            hasNext = pageIndex < res.eps.pages
            pageIndex++
        }
        return list
    }

    /**
     * 将漫画所有章节加入下载队列，返回成功加入的数量；失败时抛出异常由调用方处理。
     */
    suspend fun downloadAllEpisodes(context: Context, comicTitle: String, coverUrl: String): Int {
        val allEps = fetchAllEpisodes()
        if (allEps.isEmpty()) return 0
        
        val requests = allEps.map { episode ->
            DownloadWorker.Companion.DownloadRequest(
                comicId = id,
                comicTitle = comicTitle,
                coverUrl = coverUrl,
                episodeId = episode.id,
                episodeTitle = episode.title,
                episodeOrder = episode.order
            )
        }
        
        DownloadWorker.startDownloads(context, requests)
        return requests.size
    }

    /**
     * 将指定的漫画章节列表加入下载队列。
     */
    fun downloadEpisodes(context: Context, comicTitle: String, coverUrl: String, episodes: List<Episode>) {
        if (episodes.isEmpty()) return
        val requests = episodes.map { episode ->
            DownloadWorker.Companion.DownloadRequest(
                comicId = id,
                comicTitle = comicTitle,
                coverUrl = coverUrl,
                episodeId = episode.id,
                episodeTitle = episode.title,
                episodeOrder = episode.order
            )
        }
        DownloadWorker.startDownloads(context, requests)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            id: String,
        ): ComicInfoViewModel
    }
}