@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.comicinfo.comments

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.data.repository.CommentsRepository
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Type
import com.shizq.bika.core.network.runCatchingApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi

private val logger = KotlinLogging.logger("CommentsSM")

/**
 * 评论域状态机：输入器、点赞覆盖层、回复弹窗、置顶评论。
 *
 * 这些状态原先散在三处——`viewingReplies` 在详情状态机的 Content 里、
 * `likeOverrides` 与 `pinnedCommentsRefresh` 是 ViewModel 字段、草稿和
 * `actionState` 在 CommentsPage 的 remember 里。它们之间有真实的转移关系
 * （写评论 → 发送中 → 失败保留草稿 → 展开回复 → 在回复里再回复），
 * 分居三地的直接后果是覆盖层改了而根评论不知道。
 *
 * 只属于评论 tab 的状态不再挂在全屏 state 上，也就不再需要靠
 * `distinctUntilChanged` 去挡"漫画点赞把回复列表拉回第一页"。
 */
class CommentsStateMachine @AssistedInject constructor(
    private val network: BikaDataSource,
    private val commentsRepository: CommentsRepository,
    @Assisted private val comicId: String,
) : FlowReduxStateMachineFactory<CommentsState, CommentsAction>() {

    init {
        initializeWith { CommentsState() }
        spec {
            inState<CommentsState> {

                // ── 置顶评论 ──────────────────────────────────────────────
                // onEnter 拉一次；发表成功和显式刷新各再拉一次。
                // 失败不清空已有数据：一次网络抖动不该让已显示的置顶评论消失。
                onEnter {
                    val loaded = fetchPinned()
                    if (loaded == null) noChange() else mutate { copy(pinned = loaded) }
                }

                on<CommentsAction.RefreshPinned> {
                    val loaded = fetchPinned()
                    if (loaded == null) noChange() else mutate { copy(pinned = loaded) }
                }

                // ── 输入器 ────────────────────────────────────────────────
                on<CommentsAction.OpenMainCommentComposer> {
                    mutate { copy(composer = Composer.MainComment()) }
                }

                on<CommentsAction.OpenReplyComposer> { action ->
                    mutate { copy(composer = Composer.Reply(target = action.target)) }
                }

                on<CommentsAction.DraftChanged> { action ->
                    mutate {
                        // 输入器已关闭时忽略迟到的击键
                        updateOpenComposer { it.withDraft(action.text) }
                    }
                }

                on<CommentsAction.DismissComposer> {
                    // 草稿随输入器一起丢弃。保留草稿只在"发送失败"时发生，
                    // 主动关闭是用户明确放弃这次输入。
                    mutate { copy(composer = Composer.Closed) }
                }

                on<CommentsAction.Send> {
                    val open = snapshot.composer as? Composer.Open
                    val text = open?.draft?.trim().orEmpty()
                    if (open == null || text.isEmpty()) {
                        noChange()
                    } else {
                        // sending 置位用接收者的 composer，不写回捕获的 open：
                        // 网络往返期间草稿可能又变了
                        mutate {
                            updateOpenComposer {
                                it.withSending(true).withError(null)
                            }
                        }
                        runCatchingApi {
                            when (open) {
                                is Composer.MainComment -> {
                                    network.postComment(Type.COMIC, comicId, text)
                                }

                                is Composer.Reply -> {
                                    network.postCommentReply(open.target.rootCommentId, text)
                                }
                            }
                        }.fold(
                            onSuccess = {
                                // 成功才关闭并清空草稿；token 自增让常规列表刷新，
                                // 置顶评论在这里同步重拉（发表的可能就是置顶）
                                val loaded = fetchPinned()
                                mutate {
                                    copy(
                                        composer = Composer.Closed,
                                        pinned = loaded ?: pinned,
                                        listRefreshToken = listRefreshToken + 1,
                                    )
                                }
                            },
                            onFailure = { e ->
                                logger.error(e) { "发表评论失败" }
                                mutate {
                                    updateOpenComposer {
                                        it.withSending(false).withError(e)
                                    }
                                }
                            }
                        )
                    }
                }

                // ── 点赞（乐观更新）──────────────────────────────────────
                on<CommentsAction.ToggleLike> { action ->
                    val previous = snapshot.likeOverrides[action.commentId]
                    mutate {
                        copy(
                            likeOverrides = likeOverrides +
                                    (action.commentId to !action.currentlyLiked)
                        )
                    }
                    runCatchingApi { network.toggleCommentLike(action.commentId) }.fold(
                        onSuccess = { noChange() },
                        onFailure = { e ->
                            logger.error(e) { "评论点赞失败，回滚" }
                            // 回滚到操作前的覆盖值（可能本来就没有）。
                            // 读接收者的 likeOverrides，不是捕获的那份
                            mutate {
                                copy(
                                    likeOverrides = if (previous == null) {
                                        likeOverrides - action.commentId
                                    } else {
                                        likeOverrides + (action.commentId to previous)
                                    }
                                )
                            }
                        }
                    )
                }

                // ── 回复弹窗 ──────────────────────────────────────────────
                on<CommentsAction.ExpandReplies> { action ->
                    mutate { copy(viewingReplies = action.comment) }
                }

                on<CommentsAction.CollapseReplies> {
                    mutate {
                        copy(
                            viewingReplies = null,
                            composer = Composer.Closed,
                        )
                    }
                }
            }
        }
    }

    /**
     * 拉取置顶评论，失败返回 null（调用方保留旧值）。
     *
     * 返回原始数据、不在这里套点赞覆盖层：覆盖层只有一份，统一在读取侧
     * （ViewModel 的投影）应用。在这里先烘进去的话，后续 ToggleLike 改了
     * 覆盖层而这份已经定型，置顶区和列表就会显示不同的点赞状态。
     *
     * page 固定为 1：topComments 只随第一页返回。
     */
    private suspend fun fetchPinned(): List<Comment>? =
        runCatchingApi { commentsRepository.getCommentPage(comicId, 1).topComments }
            .onFailure { logger.error(it) { "加载置顶评论失败，保留上一次的值" } }
            .getOrNull()

    @AssistedFactory
    interface Factory {
        fun create(comicId: String): CommentsStateMachine
    }
}

private inline fun CommentsState.updateOpenComposer(
    transform: (Composer.Open) -> Composer.Open,
): CommentsState = when (val current = composer) {
    Composer.Closed -> this
    is Composer.Open -> copy(composer = transform(current))
}

private fun Composer.Open.withDraft(draft: String): Composer.Open = when (this) {
    is Composer.MainComment -> copy(draft = draft)
    is Composer.Reply -> copy(draft = draft)
}

private fun Composer.Open.withSending(sending: Boolean): Composer.Open = when (this) {
    is Composer.MainComment -> copy(sending = sending)
    is Composer.Reply -> copy(sending = sending)
}

private fun Composer.Open.withError(error: Throwable?): Composer.Open = when (this) {
    is Composer.MainComment -> copy(error = error)
    is Composer.Reply -> copy(error = error)
}
