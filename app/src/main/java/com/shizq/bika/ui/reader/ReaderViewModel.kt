@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.reader

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.data.repository.DownloadRepository
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.paging.Chapter
import com.shizq.bika.paging.ChapterListPagingSource
import com.shizq.bika.paging.ChapterMeta
import com.shizq.bika.paging.ChapterPage
import com.shizq.bika.paging.ChapterPagesPagingSource
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.state.ReaderUiState
import com.shizq.bika.ui.reader.statemachine.ReaderStateMachine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ReaderViewModel"

@HiltViewModel(assistedFactory = ReaderViewModel.Factory::class)
class ReaderViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    private val chapterPagesPagingSourceFactory: ChapterPagesPagingSource.Factory,
    private val chapterListPagingSourceFactory: ChapterListPagingSource.Factory,
    private val downloadRepository: DownloadRepository,
    private val downloadTaskRepository: DownloadTaskRepository,
    readerStateMachine: ReaderStateMachine,
    @Assisted id: String,
    @Assisted order: Int,
    @Assisted downloadedOnly: Boolean,
) : ViewModel() {
    private val currentChapterOrder = savedStateHandle.getStateFlow("order", order)

    init {
        readerStateMachine.initializeWith { ReaderUiState.Initializing(id, order) }
    }

    private val stateMachine = readerStateMachine.launchIn(viewModelScope)
    val stateFlow = stateMachine.state

    // 图片列表流：下载模式读取本地文件，在线模式从网络加载
    val imageListFlow: Flow<PagingData<ChapterPage>> =
        if (downloadedOnly) {
            // 下载模式：从本地存储读取图片文件
            currentChapterOrder.flatMapLatest { chapterOrder ->
                flow {
                    // 在 IO 线程读取本地图片文件列表
                    val localImages = withContext(Dispatchers.IO) {
                        downloadRepository.getLocalImages(id, chapterOrder)
                    }
                    // 从下载任务记录中获取章节元信息（标题、总页数）
                    val task = downloadTaskRepository
                        .observeTask("${id}_$chapterOrder")
                        .first()
                    dispatch(
                        ReaderAction.ChapterMetaLoaded(
                            ChapterMeta(
                                title = task?.episodeTitle ?: "第 $chapterOrder 话",
                                totalImages = localImages.size
                            )
                        )
                    )
                    emit(
                        PagingData.from(
                            localImages.map { file ->
                                ChapterPage(
                                    id = file.name,
                                    url = Uri.fromFile(file).toString()
                                )
                            }
                        )
                    )
                }
            }.cachedIn(viewModelScope)
        } else {
            // 在线模式：从网络加载图片
            currentChapterOrder
                .flatMapLatest { chapterOrder ->
                    Pager(PagingConfig(40)) {
                        chapterPagesPagingSourceFactory.create(id, chapterOrder) { meta ->
                            dispatch(ReaderAction.ChapterMetaLoaded(meta))
                        }
                    }.flow
                }
                .cachedIn(viewModelScope)
        }

    // 章节列表流 (用于侧边栏及上下章导航)
    // downloadedOnly=true 时只展示已下载完成的章节，限制章间导航范围
    val chapterListFlow: Flow<PagingData<Chapter>> =
        if (downloadedOnly) {
            downloadTaskRepository.observeTasksByComic(id)
                .map { tasks ->
                    PagingData.from(
                        tasks
                            .filter { it.status == DownloadStatus.COMPLETED }
                            .sortedBy { it.episodeOrder }
                            .map { task ->
                                Chapter(
                                    id = task.episodeId,
                                    order = task.episodeOrder,
                                    title = task.episodeTitle,
                                    updatedAt = ""
                                )
                            }
                    )
                }
                .cachedIn(viewModelScope)
        } else {
            Pager(config = PagingConfig(pageSize = 20)) {
                chapterListPagingSourceFactory.create(id)
            }.flow
                .cachedIn(viewModelScope)
        }

    fun dispatch(action: ReaderAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(id: String, order: Int, downloadedOnly: Boolean): ReaderViewModel
    }
}