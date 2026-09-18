@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.comicinfo.statemachine

import android.util.Log
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.runCatchingApi
import com.shizq.bika.ui.comicinfo.ComicDetail
import com.shizq.bika.ui.comicinfo.UnitedDetailsAction
import com.shizq.bika.ui.comicinfo.UnitedDetailsUiState
import com.shizq.bika.ui.comicinfo.toComicDetail
import com.shizq.bika.ui.comicinfo.toComicSummaryList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock

class UnitedDetailsStateMachine @AssistedInject constructor(
    private val network: BikaDataSource,
    private val historyDao: ReadingHistoryDao,
    // 原名 id：与"评论 id"在状态机和 ViewModel 里同名同类型，传错编译器不拦
    @Assisted private val comicId: String,
) : FlowReduxStateMachineFactory<UnitedDetailsUiState, UnitedDetailsAction>() {

    init {
        initializeWith { UnitedDetailsUiState.Initialize }
        spec {
            inState<UnitedDetailsUiState.Initialize> {
                onEnter {
                    runCatchingApi {
                        coroutineScope {
                            val detailDeferred =
                                async { network.getComicDetails(comicId).toComicDetail() }
                            val recommendationsDeferred = async {
                                network.getRecommendations(comicId).toComicSummaryList()
                            }
                            detailDeferred.await() to recommendationsDeferred.await()
                        }
                    }.fold(
                        onSuccess = { (detail, recommendations) ->
                            // 历史写入放在这里而不是 Content.onEnterEffect：
                            // 后者在每次 mutate（点赞/收藏/展开回复）后是否重新触发，
                            // 取决于 flowredux2 的 re-entry 语义。加载成功恰好发生一次，
                            // 语义明确且与原先的意图等价。
                            historyDao.upsertHistory(detail.toHistoryEntity(comicId))
                            Log.d(
                                TAG,
                                "Upsert history for '${detail.title}' with full comic details."
                            )
                            override {
                                UnitedDetailsUiState.Content(
                                    id = comicId,
                                    detail = detail,
                                    recommendations = recommendations
                                )
                            }
                        },
                        onFailure = { override { UnitedDetailsUiState.Error(it) } }
                    )
                }
            }

            inState<UnitedDetailsUiState.Content> {
                on<UnitedDetailsAction.ToggleLike> {
                    val currentDetail = snapshot.detail

                    runCatchingApi { network.toggleComicLike(snapshot.id) }.fold(
                        onSuccess = { r ->
                            val isLiked = when (r.action) {
                                ACTION_LIKE -> true
                                ACTION_UNLIKE -> false
                                else -> currentDetail.isLiked
                            }
                            mutate {
                                copy(detail = currentDetail.copy(isLiked = isLiked))
                            }
                        },
                        onFailure = { e ->
                            Log.e(TAG, "ToggleLike: ", e)
                            noChange()
                        }
                    )
                }
                on<UnitedDetailsAction.ToggleFavorite> {
                    val currentDetail = snapshot.detail

                    runCatchingApi { network.toggleComicFavourite(snapshot.id) }.fold(
                        onSuccess = { r ->
                            val isFavourited = when (r.action) {
                                ACTION_FAVORITE -> true
                                ACTION_UN_FAVORITE -> false
                                else -> currentDetail.isFavourited
                            }
                            // Room 的 suspend 方法自带调度，不需要外层再包 Dispatchers.IO
                            historyDao.updateIsFavourited(snapshot.id, isFavourited)
                            Log.d(
                                TAG,
                                "Sync isFavourited for '${snapshot.id}' to local database: $isFavourited"
                            )
                            mutate {
                                copy(detail = currentDetail.copy(isFavourited = isFavourited))
                            }
                        },
                        onFailure = { e ->
                            Log.e(TAG, "ToggleFavorite: ", e)
                            noChange()
                        }
                    )
                }
                on<UnitedDetailsAction.TopCommentsLoaded> {
                    mutate { copy(pinnedComments = it.comments) }
                }
                on<UnitedDetailsAction.ExpandReplies> {
                    mutate { copy(viewingReplies = it.comment) }
                }
                on<UnitedDetailsAction.CollapseReplies> {
                    mutate { copy(viewingReplies = null) }
                }
            }

            inState<UnitedDetailsUiState.Error> {
                on<UnitedDetailsAction.Retry> {
                    override { UnitedDetailsUiState.Initialize }
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(comicId: String): UnitedDetailsStateMachine
    }

    private companion object {
        const val ACTION_LIKE = "like"
        const val ACTION_UNLIKE = "unlike"
        const val ACTION_FAVORITE = "favourite"
        const val ACTION_UN_FAVORITE = "un_favourite"
        private const val TAG = "UnitedDetailsStateMachine"
    }
}

private fun ComicDetail.toHistoryEntity(comicId: String) = ReadingHistoryEntity(
    id = comicId,
    title = title,
    author = author,
    coverUrl = cover,
    lastInteractionAt = Clock.System.now(),
    categories = categories,
    pagesCount = pagesCount,
    epsCount = epsCount,
    finished = finished,
    totalLikes = totalLikes,
    isFavourited = isFavourited
)