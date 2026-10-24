@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.shizq.bika.feature.comicdetail.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.repository.ChapterRepository
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.message.MessageAction
import com.shizq.bika.core.message.MessageDuration
import com.shizq.bika.core.message.MessageId
import com.shizq.bika.core.message.MessageReporter
import com.shizq.bika.core.message.UiText
import com.shizq.bika.core.message.reportError
import com.shizq.bika.core.message.reportInfo
import com.shizq.bika.feature.comicdetail.impl.statemachine.UnitedDetailsStateMachine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import android.content.Context
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val logger = KotlinLogging.logger("ComicInfoViewModel")

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
    private val historyDao: ReadingHistoryDao,
    private val messageReporter: MessageReporter,
    private val comicDownloadEnqueuer: ComicDownloadEnqueuer,
    @ApplicationContext private val context: Context,
    @Assisted private val comicId: String,
) : ViewModel() {

    private val _isDownloadingWholeComic = MutableStateFlow(false)
    val isDownloadingWholeComic: StateFlow<Boolean> = _isDownloadingWholeComic.asStateFlow()

    private val stateMachine = stateMachineFactory.create(comicId).launchIn(viewModelScope)

    val state = stateMachine.state

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

    val controls = combine(state, downloadTasks, chapterProgress) { detailState, tasks, progress ->
        val detail = (detailState as? UnitedDetailsUiState.Content)?.detail
        val lastRead = progress.maxByOrNull { it.lastReadAt }
        ComicDetailControlsState(
            downloadState = if (detail == null) {
                ComicDownloadState.NotDownloaded
            } else {
                resolveComicDownloadState(tasks, detail.epsCount)
            },
            resumeChapterOrder = lastRead?.chapterId ?: 1,
            hasReadProgress = lastRead != null,
            readingStatus = detail?.let { resolveComicReadingStatus(it.epsCount, progress) },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ComicDetailControlsState(),
    )

    /**
     * 章节列表。
     *
     * 分页与页尺寸都由 [ChapterRepository] 决定：原先这里自建 Pager + EpisodePagingSource，
     * 与 repository 的 getChapterList 打同一个端点做同一件事，两份实现已经分叉。
     *
     * 这里刻意不叠加 `cachedIn`：目录 tab 的订阅者会随详情页一起离开组合（进阅读器时），
     * 返回时再重新订阅同一份缓存。只要那一代分页在订阅中断时停在 refresh Loading 或 Error
     * 且一页都没拿到，重放出来的历史就仍然只有「空 + 未结束的加载状态」，而真正推进它的
     * PageFetcher 已经没有人消费，UI 便会永远停在「正在获取章节列表…」。
     * 每次订阅都新建 Pager 可以保证回到目录页时一定重新发起一次真实请求；
     * 代价仅是旋转屏幕等重新组合的场景会重新拉第一页（章节列表就是一个轻量请求）。
     */
    private val episodesReloadTrigger = MutableStateFlow(0)

    val episodesFlow: Flow<PagingData<Chapter>> = episodesReloadTrigger
        .flatMapLatest { chapterRepository.getChapterList(comicId) }

    /**
     * 重建章节分页流，供目录页的「重试 / 重新加载」使用。
     *
     * 卡住或失败的那一代分页可能已经没有活跃的 PageFetcher，此时
     * `LazyPagingItems.retry()` / `refresh()` 只是把加载提示发给一个不再产出事件的
     * 数据源，不会真的重新请求。换掉触发值让上层重新收集一条全新的 Pager 流，
     * 是这类状态下唯一可靠的自救方式。
     */
    fun reloadEpisodes() {
        episodesReloadTrigger.update { it + 1 }
    }

    fun dispatch(action: UnitedDetailsAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }

    private suspend fun fetchAllEpisodes(): List<Chapter> =
        chapterRepository.getAllChapters(comicId)

    /**
     * 下载整本漫画。
     *
     * ## 为什么用 viewModelScope 而不是让 UI 自己起协程
     *
     * 拉取完整章节列表要按页请求，耗时可达数秒。原实现由详情页在
     * `rememberCoroutineScope()` 里调用，而那个 scope 属于 pager 的 DETAIL 分页——
     * 用户在等待期间切到「目录」，分页被 dispose，协程随之取消，
     * 于是既没有下载也没有任何提示。入队是一次「发起后就该完成」的动作，
     * 生命周期必须跟着 ViewModel。
     *
     * ## 为什么单话分支在这里
     *
     * 「epsCount <= 1 视为单话」是下载域的规则，与 [isComicFullyDownloaded]
     * 的同名分支必须同步演化。放在 UI 的 onClick 里，两处会各自漂移，
     * 表现为「显示已下载，但点下载又入队一遍」。
     */
    fun downloadWholeComic(comicTitle: String, coverUrl: String, epsCount: Int) {
        if (_isDownloadingWholeComic.value) return
        viewModelScope.launch {
            _isDownloadingWholeComic.value = true
            Toast.makeText(context, "正在获取章节信息...", Toast.LENGTH_SHORT).show()

            if (epsCount <= 1) {
                try {
                    enqueueEpisodes(comicTitle, coverUrl, listOf(singleEpisodeChapter()))
                    Toast.makeText(context, "已加入下载队列（全一话）", Toast.LENGTH_SHORT).show()
                    messageReporter.reportInfo(UiText.of(R.string.download_enqueued))
                } finally {
                    _isDownloadingWholeComic.value = false
                }
                return@launch
            }

            val pendingId = MessageId("download_all_$comicId")
            messageReporter.reportInfo(
                UiText.of(R.string.download_fetching_chapters),
                id = pendingId,
            )
            try {
                val allEps = fetchAllEpisodes()
                if (allEps.isEmpty()) {
                    messageReporter.dismiss(pendingId)
                    Toast.makeText(context, "未获取到可用章节", Toast.LENGTH_SHORT).show()
                    messageReporter.reportError(UiText.of(R.string.download_no_chapters))
                    return@launch
                }
                enqueueEpisodes(comicTitle, coverUrl, allEps)
                messageReporter.dismiss(pendingId)
                val successMsg = "已成功将 ${allEps.size} 个章节加入下载队列"
                Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                messageReporter.reportInfo(
                    UiText.of(R.string.download_all_enqueued, allEps.size),
                    duration = MessageDuration.Long,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "获取全部章节失败: comicId=$comicId" }
                messageReporter.dismiss(pendingId)
                Toast.makeText(context, "获取章节失败，请稍后重试", Toast.LENGTH_SHORT).show()
                messageReporter.reportError(
                    UiText.of(R.string.download_fetch_chapters_failed),
                    duration = MessageDuration.Long,
                    action = MessageAction(
                        label = UiText.of(R.string.retry),
                        onPerformed = { downloadWholeComic(comicTitle, coverUrl, epsCount) },
                    ),
                )
            } finally {
                _isDownloadingWholeComic.value = false
            }
        }
    }

    /**
     * 批量写库后一次性触发调度。
     *
     * 任务创建和调度由 [ComicDownloadEnqueuer] 统一负责，避免整本下载和选择下载
     * 各自维护一份实现。
     */
    private suspend fun enqueueEpisodes(
        comicTitle: String,
        coverUrl: String,
        episodes: List<Chapter>,
    ) {
        comicDownloadEnqueuer.enqueue(comicId, comicTitle, coverUrl, episodes)
    }

    companion object {
        /** 生成任务 ID，与旧 DownloadRepository.taskId 保持一致 */
        fun taskId(comicId: String, episodeOrder: Int) =
            ComicDownloadEnqueuer.taskId(comicId, episodeOrder)

        /**
         * 单话漫画的合成章节。
         *
         * 服务端对这类漫画不返回章节列表，但下载任务需要一个 order 才能定位图片
         * （[com.shizq.bika.core.download.executor.ChapterDownloadExecutor] 用
         * comicId + episodeOrder 请求，不读 episodeId）。order = 1 与
         * [isComicFullyDownloaded] 的单话分支一致。
         */
        internal fun singleEpisodeChapter() = Chapter(
            id = "single_episode",
            title = "全一话",
            order = 1,
            updatedAt = "",
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(
            comicId: String,
        ): ComicInfoViewModel
    }
}
