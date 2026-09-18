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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

private const val TAG = "ComicInfoViewModel"

/** 评论每页条数，与服务端 comments 接口一致 */
private const val PAGE_SIZE_COMMENT = 40

/**
 * 回复每页条数，保持原值 5。
 *
 * 它可能与服务端每页实际返回条数（评论侧是 40）不符，会让 Paging 更频繁
 * 触发下一页。但改动它会改变请求量，属于需要单独测量的决策，不搭本次重构的车。
 */
private const val PAGE_SIZE_REPLY = 5

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

    val regularComments: Flow<PagingData<Comment>> = Pager(PagingConfig(PAGE_SIZE_COMMENT)) {
        commentPagingSourceFactory(comicId, ::onTopCommentsLoaded)
    }.flow
        // cachedIn 必须在 combine/map 之前，否则缓存失效、滑动时会重复请求
        .cachedIn(viewModelScope)
        .combine(likeOverrides) { paging, overrides ->
            paging.map { it.applyLikeOverride(overrides) }
        }

    val replyList = state
        .viewingRepliesIds()
        .flatMapLatest { commentId ->
            // 原先用 filterNotNull，关闭弹窗后回不到"无回复在看"的状态
            if (commentId == null) {
                flowOf(PagingData.empty())
            } else {
                Pager(PagingConfig(PAGE_SIZE_REPLY)) {
                    replyPagingSourceFactory(commentId)
                }.flow
            }
        }
        .cachedIn(viewModelScope)
        .combine(likeOverrides) { paging, overrides ->
            paging.map { it.applyLikeOverride(overrides) }
        }

    /**
     * 置顶评论走 dispatch 而不是直写 StateFlow，保证 Content 只有状态机一个写入者。
     *
     * 注意时序：评论第一页与漫画详情是并发请求的（collectAsLazyPagingItems 在
     * ComicDetailScreen 顶层调用，不在评论 tab 里）。若评论先返回，状态机还在
     * Initialize，没有匹配的 inState，这个 action 会被丢弃、置顶评论静默消失。
     * 所以缓存最后一次结果，进入 Content 时由下面的 init 补发一次。
     */
    @Volatile
    private var lastTopComments: List<Comment>? = null

    private fun onTopCommentsLoaded(comments: List<Comment>) {
        lastTopComments = comments
        dispatch(UnitedDetailsAction.TopCommentsLoaded(comments))
    }

    init {
        // Initialize -> Content 的瞬间补发一次，覆盖"评论比详情先返回"的情况
        viewModelScope.launch {
            state.filterIsInstance<UnitedDetailsUiState.Content>()
                .take(1)
                .collect { content ->
                    if (shouldReplayTopComments(lastTopComments, content.pinnedComments)) {
                        stateMachine.dispatch(
                            UnitedDetailsAction.TopCommentsLoaded(lastTopComments!!)
                        )
                    }
                }
        }
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

    fun postComment(text: String, replyToCommentId: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (replyToCommentId == null) {
                    network.postComment(Type.COMIC, comicId, text)
                } else {
                    network.postCommentReply(replyToCommentId, text)
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
     */
    suspend fun fetchAllEpisodes(): List<Episode> {
        val list = mutableListOf<Episode>()
        var pageIndex = 1
        var hasNext = true
        while (hasNext) {
            val res = network.getComicEpisodes(comicId, pageIndex)
            list.addAll(res.eps.docs)
            hasNext = pageIndex < res.eps.pages
            pageIndex++
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
 * 首次进入 Content 时是否需要补发一次置顶评论。
 *
 * 背景：评论第一页与漫画详情并发请求。若评论先返回，状态机还在 Initialize，
 * 没有匹配的 inState，[UnitedDetailsAction.TopCommentsLoaded] 会被静默丢弃，
 * 置顶评论永远不出现。所以缓存最后一次回调结果，进入 Content 时补发。
 *
 * 只在"有缓存且与状态里的值不同"时补发：详情先返回的正常路径下
 * 状态里已经是正确值，此时补发只会多一次无意义的 mutate。
 *
 * @param cached PagingSource 最后一次回调带回的置顶评论，null 表示评论还没加载完
 * @param inState 状态机当前持有的置顶评论
 */
internal fun shouldReplayTopComments(
    cached: List<Comment>?,
    inState: List<Comment>,
): Boolean = cached != null && !cached.sameCommentIdsAs(inState)

/**
 * 按 id 序列比较两份评论列表。
 *
 * 不能直接用 `==`：[Comment.user] 是普通 class，没有实现 equals，
 * 两次网络解析出的 User 实例永不相等，列表比较会恒为 false、每次都补发。
 */
private fun List<Comment>.sameCommentIdsAs(other: List<Comment>): Boolean =
    size == other.size && indices.all { this[it].id == other[it].id }

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
