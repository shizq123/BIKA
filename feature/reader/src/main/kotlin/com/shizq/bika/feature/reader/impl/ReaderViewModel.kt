@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

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
import com.shizq.bika.feature.reader.impl.progress.AwaitDataRestoreStrategy
import com.shizq.bika.feature.reader.impl.progress.ProgressConfig
import com.shizq.bika.feature.reader.impl.progress.ReadingProgressManager
import com.shizq.bika.feature.reader.impl.progress.ReadingProgressWriter
import com.shizq.bika.feature.reader.impl.progress.StoreBackedSink
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
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
    progressStore: ReadingProgressStore,
    @Assisted id: String,
    @Assisted order: Int,
    @Assisted downloadedOnly: Boolean,
) : ViewModel() {
    private val currentChapterOrder = savedStateHandle.getStateFlow("order", order)

    /**
     * 进度管理器归 ViewModel，用 viewModelScope。
     *
     * 这是本次重写的核心结构改动：旧实现在 composition 里用 rememberCoroutineScope()
     * 持有它，于是「必须比组合活得久的写入」和「必须随组合销毁的观察」共用一个 scope，
     * 组合销毁会掐死尚未触发的防抖 job。ReadingProgressManager 的 KDoc 里那段
     * 「onPersist 不能挂起、必须转交 viewModelScope」的契约、以及
     * persistLastKnownPage 这个同步逃生口，都是为补偿这一点而存在的。
     *
     * 现在写入在 viewModelScope 里，观察由 composition 通过 ReadingProgressEffect
     * 驱动（那部分随组合取消是正确的，controller 本就与组合同生死）。
     */
    private val progressConfig = ProgressConfig()

    val progressManager = ReadingProgressManager(
        writer = ReadingProgressWriter(
            sink = StoreBackedSink(progressStore),
            scope = viewModelScope,
            debounce = progressConfig.persistDebounce,
        ),
        restoreStrategy = AwaitDataRestoreStrategy(),
        config = progressConfig,
    )

    init {
        readerStateMachine.initializeWith { ReaderUiState.Initializing(id, order) }
        // 把切章写入接到流水线上。必须在 launchIn 之前完成，否则第一次
        // JumpToChapter 可能落到 NoOp 上。
        readerStateMachine.progressWriteCoordinator = progressManager
    }

    private val stateMachine = readerStateMachine.launchIn(viewModelScope)
    val stateFlow = stateMachine.state

    // 在线模式下，每次章节变化只调用一次 getChapterPages，pages 和 meta 共享同一个结果，
    // 用 shareIn 转为热流，避免 meta/pages 各自订阅时分别触发一次网络请求。
    //
    // 分页流同时以 order 和 initialPage 作为 key：initialPage 只在从
    // JumpToChapter 派生出新的 ChapterState 时变化一次，不会随后续翻页而变，
    // 因此不会导致同一章节内翻页时反复重建分页流。
    // 该值被传给 getChapterPages 用于换算首次请求的 API 页（见 ChapterRepositoryImpl），
    // 使恢复到很靠后的页时无需逐页向前加载。
    //
    // 只从 Ready 状态派生：Initializing 阶段 initialPage 尚未从数据库查出（固定为占位值），
    // 若把它也纳入 key，会在 Initializing -> Ready 转换时把 (order, 占位值) 和
    // (order, 真实值) 当成两个不同的 key，导致启动时多打一次浪费的网络请求。
    private val chapterPagesResultFlow = stateMachine.state
        .filterIsInstance<ReaderUiState.Ready>()
        .map { state -> state.chapter.order to state.chapter.initialPage }
        .distinctUntilChanged()
        .map { (chapterOrder, initialPage) ->
            chapterRepository.getChapterPages(id, chapterOrder, initialPage)
        }
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