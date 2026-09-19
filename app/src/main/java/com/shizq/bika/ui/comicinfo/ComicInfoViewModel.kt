@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.comicinfo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.scheduler.DownloadScheduler
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.core.network.model.Type
import com.shizq.bika.core.network.runCatchingApi
import com.shizq.bika.paging.EpisodePagingSource
import com.shizq.bika.ui.comicinfo.paging.CommentPagingSource
import com.shizq.bika.ui.comicinfo.paging.ReplyPagingSource
import com.shizq.bika.ui.comicinfo.statemachine.UnitedDetailsStateMachine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

private const val TAG = "ComicInfoViewModel"

/** 评论每页条数，与服务端 comments 接口一致 */
private const val PAGE_SIZE_COMMENT = 40

/**
 * 回复每页条数。
 *
 * 与评论保持一致：服务端 comments 系列接口一页给 40 条，客户端声明 5
 * 会让 Paging 基于错误的页尺寸做预取判断，配合去重后的短页更难预测。
 */
private const val PAGE_SIZE_REPLY = PAGE_SIZE_COMMENT

/**
 * [ComicInfoViewModel.fetchAllEpisodes] 的页数硬上限。
 *
 * 服务端一页 40 话，500 页足够覆盖任何真实漫画；它的作用是在 `pages`
 * 字段不可信时给循环一个确定的终点，而不是限制正常数据。
 */
private const val MAX_EPISODE_PAGES = 500

@HiltViewModel(assistedFactory = ComicInfoViewModel.Factory::class)
class ComicInfoViewModel @AssistedInject constructor(
    private val network: BikaDataSource,
    private val commentPagingSourceFactory: CommentPagingSource.Factory,
    private val replyPagingSourceFactory: ReplyPagingSource.Factory,
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

    val episodesFlow: Flow<PagingData<Episode>> = Pager(PagingConfig(40)) {
        EpisodePagingSource(network, comicId)
    }
        .flow
        .cachedIn(viewModelScope)

    /**
     * 评论点赞的本地覆盖层。
     *
     * PagingData 不可变，点赞结果没法写回已加载的页，所以用一层
     * commentId -> 用户切换后的状态 的覆盖，在 map 阶段合并进列表。
     * 请求失败时把条目移除即回滚。
     */
    private val likeOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    val regularComments: Flow<PagingData<Comment>> = Pager(
        // enablePlaceholders = false：跨页去重会让单页条数小于 pageSize，
        // 占位符的位置计算失去依据（PagingSource 也给不出准确的 itemsBefore/After）
        PagingConfig(pageSize = PAGE_SIZE_COMMENT, enablePlaceholders = false)
    ) {
        commentPagingSourceFactory(comicId)
    }.flow
        // cachedIn 必须在 combine/map 之前，否则缓存失效、滑动时会重复请求
        .cachedIn(viewModelScope)
        .combine(likeOverrides) { paging, overrides ->
            paging.map { it.applyLikeOverride(overrides) }
        }

    /** 置顶评论的刷新信号，发表评论成功后与常规列表一起刷新 */
    private val pinnedCommentsRefresh = MutableStateFlow(0)

    /**
     * 置顶评论。
     *
     * 独立于 [regularComments] 请求，不走分页源的旁路回调。置顶评论不分页、
     * 与页码无关，塞进 PagingSource 只能靠回调往外递，而 Paging 的缓存重放
     * 不会重跑 `load`，重新订阅时这份数据就丢了。独立成流后它有自己的生命周期，
     * 也能自然地套上点赞覆盖层。
     *
     * 由 UI 直接收集，不再经状态机的 TopCommentsLoaded：置顶评论是网络数据而非
     * UI 交互状态，绕状态机一圈还得为"Initialize 期间 action 被丢弃"补一套补发逻辑。
     *
     * 失败时不发射，保留上一次的值：一次网络抖动不该让已显示的置顶评论消失，
     * 列表自身的错误态已由 Paging 呈现。
     */
    val pinnedComments: StateFlow<List<Comment>> = pinnedCommentsRefresh
        .flatMapLatest {
            flow {
                // page 固定为 1：topComments 只随第一页返回，与分页无关
                val response = network.getComments(Type.COMIC, comicId, 1)
                emit(response.topComments.map { it.asExternalModel() })
            }.catch { e ->
                if (e is CancellationException) throw e
                Log.e(TAG, "load pinned comments failed", e)
            }
        }
        .combine(likeOverrides) { pinned, overrides ->
            pinned.map { it.applyLikeOverride(overrides) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val replyList = state
        .viewingRepliesIds()
        .flatMapLatest { commentId ->
            // 原先用 filterNotNull，关闭弹窗后回不到"无回复在看"的状态
            if (commentId == null) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    PagingConfig(pageSize = PAGE_SIZE_REPLY, enablePlaceholders = false)
                ) {
                    replyPagingSourceFactory(commentId)
                }.flow
                    // cachedIn 放在 flatMapLatest 内部：放外层时所有 commentId
                    // 共用同一个缓存槽，切换根评论后可能先看到上一条的缓存页
                    .cachedIn(viewModelScope)
            }
        }
        .combine(likeOverrides) { paging, overrides ->
            paging.map { it.applyLikeOverride(overrides) }
        }

    fun dispatch(action: UnitedDetailsAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }

    /**
     * 切换评论点赞。
     *
     * @param commentId 参数原名 id，与构造参数里的漫画 id 同名同类型，传错编译器不会拦
     * @param currentlyLiked 列表当前显示的点赞状态。必须由调用方传入：
     *   真实状态在 PagingData 里，覆盖层只记录"用户改成了什么"，
     *   单靠覆盖层无法区分"未操作过的已点赞"和"未操作过的未点赞"。
     */
    fun toggleCommentLike(commentId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            val previous = likeOverrides.value[commentId]
            // 乐观更新：先动 UI，失败再回滚到操作前的覆盖值（可能本来就没有）
            likeOverrides.update { it + (commentId to !currentlyLiked) }
            runCatchingApi { network.toggleCommentLike(commentId) }
                .onFailure { e ->
                    Log.e(TAG, "toggleCommentLike: ", e)
                    likeOverrides.update {
                        if (previous == null) it - commentId else it + (commentId to previous)
                    }
                }
        }
    }

    /** 重新拉取置顶评论，供发表评论成功后与常规列表一起刷新 */
    fun refreshPinnedComments() {
        pinnedCommentsRefresh.update { it + 1 }
    }

    fun postComment(text: String, replyToCommentId: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (replyToCommentId == null) {
                    network.postComment(Type.COMIC, comicId, text)
                } else {
                    network.postCommentReply(replyToCommentId, text)
                }
                onResult(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "postComment failed", e)
                onResult(false)
            }
        }
    }

    /**
     * 获取漫画所有章节列表（用于 EpisodesPage 下载选择面板）。
     *
     * 两处防止打空转的约束：空页立即停（`pages` 虚高时否则永不满足退出条件），
     * 以及 [MAX_EPISODE_PAGES] 硬上限。异常直接抛给调用方，由 UI 的 catch 提示重试。
     */
    suspend fun fetchAllEpisodes(): List<Episode> {
        val list = mutableListOf<Episode>()
        var pageIndex = 1
        var hasNext = true
        while (hasNext && pageIndex <= MAX_EPISODE_PAGES) {
            val res = network.getComicEpisodes(comicId, pageIndex)
            val docs = res.eps.docs
            list.addAll(docs)
            hasNext = docs.isNotEmpty() && pageIndex < res.eps.pages
            pageIndex++
        }
        if (hasNext) {
            Log.w(TAG, "fetchAllEpisodes 到达 $MAX_EPISODE_PAGES 页上限，章节可能不完整")
        }
        return list
    }

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
    fun downloadEpisodes(comicTitle: String, coverUrl: String, episodes: List<Episode>) {
        if (episodes.isEmpty()) return
        viewModelScope.launch {
            enqueueEpisodes(comicTitle, coverUrl, episodes)
        }
    }

    private suspend fun enqueueEpisodes(
        comicTitle: String,
        coverUrl: String,
        episodes: List<Episode>,
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

/**
 * 从 UI 状态流里提取"当前在看哪条评论的回复"，null 表示弹窗关闭。
 *
 * [distinctUntilChanged] 是必需的而非优化：state 每次 mutate（漫画点赞、收藏、
 * 置顶评论到达）都会发射新值，下游的 flatMapLatest 会因此取消并重建回复 Pager，
 * 回复列表被反复拉回第一页。
 *
 * internal 而非内联在 replyList 里：需要被单测覆盖。
 */
internal fun Flow<UnitedDetailsUiState>.viewingRepliesIds(): Flow<String?> =
    map { (it as? UnitedDetailsUiState.Content)?.viewingReplies?.id }
        .distinctUntilChanged()

/**
 * 把点赞覆盖层合并进单条评论。
 *
 * 只在"覆盖值与原始值不同"时才改动计数，避免同一次点赞被重复计入：
 * 服务端刷新后返回的新数据里 isLiked 已经是点赞后的值，此时覆盖层仍在，
 * 若无条件加一，计数就会比真实值多一。
 *
 * internal 而非 private：需要被单测覆盖。
 */
internal fun Comment.applyLikeOverride(overrides: Map<String, Boolean>): Comment {
    val overridden = overrides[id] ?: return this
    if (overridden == isLiked) return this
    return copy(
        isLiked = overridden,
        likesCount = (likesCount + if (overridden) 1 else -1).coerceAtLeast(0)
    )
}
