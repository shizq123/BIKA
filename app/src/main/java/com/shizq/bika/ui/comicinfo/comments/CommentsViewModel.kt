package com.shizq.bika.ui.comicinfo.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.ui.comicinfo.paging.CommentPagingSource
import com.shizq.bika.ui.comicinfo.paging.ReplyPagingSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
 * 评论域 ViewModel。
 *
 * 从 [com.shizq.bika.ui.comicinfo.ComicInfoViewModel] 拆出来：后者原先同时管
 * 详情、章节、下载、进度、评论五块，评论部分与其余四块没有任何共享状态。
 *
 * 作用域仍是 NavEntry（由 `hiltViewModel()` 提供），不是 composition：
 * [CommentsState] 里的草稿、点赞覆盖层、回复弹窗都要在切 tab 与转屏后存活，
 * 而 HorizontalPager 会 dispose 屏幕外的页。
 */
@HiltViewModel(assistedFactory = CommentsViewModel.Factory::class)
class CommentsViewModel @AssistedInject constructor(
    private val commentPagingSourceFactory: CommentPagingSource.Factory,
    private val replyPagingSourceFactory: ReplyPagingSource.Factory,
    stateMachineFactory: CommentsStateMachine.Factory,
    @Assisted private val comicId: String,
) : ViewModel() {

    private val stateMachine = stateMachineFactory.create(comicId).launchIn(viewModelScope)

    /**
     * 向 UI 暴露的状态。
     *
     * 置顶评论与回复弹窗的根评论在这里过一遍点赞覆盖层——状态机存的是服务端原始值，
     * 覆盖层只有一份、统一在读取侧应用。不这么做的话弹窗顶部的爱心点了不动。
     */
    val state: StateFlow<CommentsState> = stateMachine.state
        .map { it.applyLikeOverrides() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CommentsState(),
        )

    /**
     * 覆盖层单独投影一条流给分页列表用。
     *
     * [distinctUntilChanged] 是必需的：state 每次 mutate（打开输入器、每一次击键）
     * 都会发射新值，下游 combine 会因此重跑一次全列表的 map。
     */
    private val likeOverrides: Flow<Map<String, Boolean>> = state
        .map { it.likeOverrides }
        .distinctUntilChanged()

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

    val replyList: Flow<PagingData<Comment>> = state
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

    fun dispatch(action: CommentsAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(comicId: String): CommentsViewModel
    }
}

/**
 * 把点赞覆盖层套到状态里的评论上。
 *
 * 只作用于 [CommentsState.pinned] 与 [CommentsState.viewingReplies]：常规列表和
 * 回复列表在 PagingData 里，由各自的 map 处理。
 *
 * 幂等：[applyLikeOverride] 在覆盖值与当前值相同时原样返回，所以重复投影不会重复计数。
 *
 * internal 而非 private：需要被单测覆盖。
 */
internal fun CommentsState.applyLikeOverrides(): CommentsState {
    if (likeOverrides.isEmpty()) return this
    return copy(
        pinned = pinned.map { it.applyLikeOverride(likeOverrides) },
        viewingReplies = viewingReplies?.applyLikeOverride(likeOverrides),
    )
}

/**
 * 从状态流里提取"当前在看哪条评论的回复"，null 表示弹窗关闭。
 *
 * [distinctUntilChanged] 是必需的而非优化：state 每次 mutate（置顶评论到达、
 * 每一次击键）都会发射新值，下游的 flatMapLatest 会因此取消并重建回复 Pager，
 * 回复列表被反复拉回第一页。
 *
 * internal 而非内联：需要被单测覆盖。
 */
internal fun Flow<CommentsState>.viewingRepliesIds(): Flow<String?> =
    map { it.viewingReplies?.id }
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
