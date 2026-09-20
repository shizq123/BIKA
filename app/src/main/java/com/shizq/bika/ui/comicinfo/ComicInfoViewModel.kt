@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.comicinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shizq.bika.R
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.repository.ChapterRepository
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.scheduler.DownloadScheduler
import com.shizq.bika.core.message.MessageAction
import com.shizq.bika.core.message.MessageDuration
import com.shizq.bika.core.message.MessageId
import com.shizq.bika.core.message.MessageReporter
import com.shizq.bika.core.message.UiText
import com.shizq.bika.core.message.reportError
import com.shizq.bika.core.message.reportInfo
import com.shizq.bika.ui.comicinfo.statemachine.UnitedDetailsStateMachine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

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
    private val downloadScheduler: DownloadScheduler,
    private val historyDao: ReadingHistoryDao,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val messageReporter: MessageReporter,
    @Assisted private val comicId: String,
) : ViewModel() {

    private val stateMachine = stateMachineFactory.create(comicId).launchIn(viewModelScope)

    val state = stateMachine.state

    fun addBlockedTag(tag: String) {
        viewModelScope.launch {
            userPreferencesDataSource.addBlockedTag(tag)
            messageReporter.reportInfo(UiText.of(R.string.tag_blocked, tag))
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
     * 与章节目录共用同一份实现。
     *
     * 这里保留 suspend 交给 UI 调用是有意的：它只为「展开选择面板」取数，
     * 面板关掉就该停止取数，挂在 composition 上的取消语义正是想要的。
     * 下载入队则相反，见 [downloadWholeComic]。
     */
    private suspend fun fetchAllEpisodes(): List<Chapter> =
        chapterRepository.getAllChapters(comicId)

    /**
     * 为章节选择面板取数，失败时上报提示并返回 null。
     *
     * 与 [fetchAllEpisodes] 的差别只在错误处理的归属：原先面板自己 try/catch，
     * 失败只写日志、把列表留在 null，于是界面显示「未找到可选章节」——
     * 与「这本漫画确实没有章节」无法区分，用户也没有重试入口。
     * 返回 null 表示「失败」，空列表表示「确实没有」，两者由类型区分开。
     *
     * 仍是 suspend：取消语义要跟着面板，见 [fetchAllEpisodes] 的说明。
     * [CancellationException] 必须先于 Exception 捕获并重抛，
     * 否则关闭面板触发的正常取消会被当成错误弹提示。
     */
    suspend fun loadSelectableEpisodes(): List<Chapter>? = try {
        fetchAllEpisodes()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.error(e) { "获取可选章节失败: comicId=$comicId" }
        messageReporter.reportError(
            UiText.of(R.string.download_fetch_chapters_failed),
            duration = MessageDuration.Long,
        )
        null
    }

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
        viewModelScope.launch {
            if (epsCount <= 1) {
                enqueueEpisodes(comicTitle, coverUrl, listOf(singleEpisodeChapter()))
                messageReporter.reportInfo(UiText.of(R.string.download_enqueued))
                return@launch
            }

            // id 固定：连点「全部下载」时后续上报被合并，不会堆出一串相同提示
            val pendingId = MessageId("download_all_$comicId")
            messageReporter.reportInfo(
                UiText.of(R.string.download_fetching_chapters),
                id = pendingId,
            )
            try {
                val allEps = fetchAllEpisodes()
                if (allEps.isEmpty()) {
                    messageReporter.dismiss(pendingId)
                    messageReporter.reportError(UiText.of(R.string.download_no_chapters))
                    return@launch
                }
                enqueueEpisodes(comicTitle, coverUrl, allEps)
                // 先撤掉「正在获取」：它没被撤时结果消息要排在其整个展示时长之后，
                // 章节拉得快的话用户会先盯着过期提示看完才见到结果
                messageReporter.dismiss(pendingId)
                messageReporter.reportInfo(
                    UiText.of(R.string.download_all_enqueued, allEps.size),
                    duration = MessageDuration.Long,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "获取全部章节失败: comicId=$comicId" }
                messageReporter.dismiss(pendingId)
                messageReporter.reportError(
                    UiText.of(R.string.download_fetch_chapters_failed),
                    duration = MessageDuration.Long,
                    action = MessageAction(
                        label = UiText.of(R.string.retry),
                        onPerformed = { downloadWholeComic(comicTitle, coverUrl, epsCount) },
                    ),
                )
            }
        }
    }

    /**
     * 将指定的漫画章节列表加入下载队列。
     */
    fun downloadEpisodes(comicTitle: String, coverUrl: String, episodes: List<Chapter>) {
        if (episodes.isEmpty()) return
        viewModelScope.launch {
            enqueueEpisodes(comicTitle, coverUrl, episodes)
            val message = if (episodes.size == 1) {
                UiText.of(R.string.download_enqueued)
            } else {
                UiText.of(R.string.download_episodes_enqueued, episodes.size)
            }
            messageReporter.reportInfo(message)
        }
    }

    /**
     * 批量写库后一次性触发调度。
     *
     * 原实现逐条 `saveTask` + `enqueue`，两个 suspend 调用串在 forEach 里：
     * 整本下载时是 2N 次往返，N 可以到几百。更要紧的是
     * [DownloadScheduler.enqueue] 每次都会以 REPLACE 策略重建 DispatchWork，
     * 逐条调用等于让后一次不断取消前一次，
     * 这正是 [DownloadScheduler.enqueueAll] 存在的理由。
     */
    private suspend fun enqueueEpisodes(
        comicTitle: String,
        coverUrl: String,
        episodes: List<Chapter>,
    ) {
        val now = Clock.System.now()
        val tasks = episodes.map { episode ->
            DownloadTask(
                id = taskId(comicId, episode.order),
                comicId = comicId,
                comicTitle = comicTitle,
                coverUrl = coverUrl,
                episodeId = episode.id,
                episodeTitle = episode.title,
                episodeOrder = episode.order,
                createdAt = now,
                updatedAt = now,
            )
        }
        downloadTaskRepository.saveTasks(tasks)
        downloadScheduler.enqueueAll(tasks.map { it.id })
    }

    companion object {
        /** 生成任务 ID，与旧 DownloadRepository.taskId 保持一致 */
        fun taskId(comicId: String, episodeOrder: Int) = "${comicId}_$episodeOrder"

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
