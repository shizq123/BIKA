package com.shizq.bika.feature.reader.impl

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.model.ChapterCatalog
import com.shizq.bika.core.data.paging.ChapterMeta
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.data.repository.ChapterRepository
import com.shizq.bika.core.data.repository.DownloadRepository
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.feature.reader.impl.state.ReaderAction
import com.shizq.bika.feature.reader.impl.state.ReaderUiState
import com.shizq.bika.feature.reader.impl.statemachine.ReaderStateMachine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = ReaderViewModel.Factory::class)
class ReaderViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    private val chapterRepository: ChapterRepository,
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

    // 在线模式下，每次章节变化只调用一次 getChapterPages，pages 和 meta 共享同一个结果，
    // 用 shareIn 转为热流，避免 meta/pages 各自订阅时分别触发一次网络请求。
    private val chapterPagesResultFlow = currentChapterOrder
        .map { chapterOrder -> chapterRepository.getChapterPages(id, chapterOrder) }
        .shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

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
            // 在线模式：从网络加载图片。章节元信息（标题、总页数）随图片分页请求一并返回，
            // 与图片流分开订阅：meta 只需消费一次副作用（dispatch），pages 交给 UI 层分页展示。
            chapterPagesResultFlow
                .flatMapLatest { it.meta }
                .onEach { meta -> dispatch(ReaderAction.ChapterMetaLoaded(meta)) }
                .launchIn(viewModelScope)

            chapterPagesResultFlow
                .flatMapLatest { it.pages }
                .cachedIn(viewModelScope)
        }

    // 章节列表流 (用于侧边栏及上下章导航)
    // downloadedOnly=true 时只展示已下载完成的章节，限制章间导航范围
    val chapterListFlow: Flow<PagingData<Chapter>> =
        if (downloadedOnly) {
            downloadedChapters(id).map { PagingData.from(it) }.cachedIn(viewModelScope)
        } else {
            chapterRepository.getChapterList(id)
                .cachedIn(viewModelScope)
        }

    // 章节目录流（全量、非分页，用于上下章导航）。
    // downloadedOnly=true 时目录来源与 chapterListFlow 一致（仅已下载完成的章节，视为“已拉全”）；
    // 在线模式复用 ChapterRepository.getChapterCatalog，内部循环拉取直至拉全。
    init {
        val catalogFlow: Flow<ChapterCatalog> = if (downloadedOnly) {
            downloadedChapters(id).map { ChapterCatalog(chapters = it, isComplete = true) }
        } else {
            chapterRepository.getChapterCatalog(id)
        }
        catalogFlow
            .onEach { dispatch(ReaderAction.ChapterCatalogLoaded(it)) }
            .launchIn(viewModelScope)
    }

    /**
     * 下载模式下，从下载任务记录派生出“已下载完成”的章节列表（按 order 升序）。
     * [chapterListFlow]（分页展示）与目录流（上下章导航）共用同一份数据来源。
     */
    private fun downloadedChapters(comicId: String): Flow<List<Chapter>> =
        downloadTaskRepository.observeTasksByComic(comicId).map { tasks ->
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