@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.comicinfo.statemachine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import android.util.Log
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.ui.comicinfo.UnitedDetailsAction
import com.shizq.bika.ui.comicinfo.UnitedDetailsUiState
import com.shizq.bika.ui.comicinfo.toComicDetail
import com.shizq.bika.ui.comicinfo.toComicSummaryList
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class UnitedDetailsStateMachine @AssistedInject constructor(
    private val network: BikaDataSource,
    private val historyDao: ReadingHistoryDao,
    @Assisted private val id: String,
) : FlowReduxStateMachineFactory<UnitedDetailsUiState, UnitedDetailsAction>() {

    init {
        initializeWith { UnitedDetailsUiState.Initialize }
        spec {
            inState<UnitedDetailsUiState.Initialize> {
                onEnter {
                    try {
                        val (detail, recommendations) = coroutineScope {
                            val detailDeferred =
                                async { network.getComicDetails(id).toComicDetail() }
                            val recommendationsDeferred = async {
                                network.getRecommendations(id).toComicSummaryList()
                            }
                            detailDeferred.await() to recommendationsDeferred.await()
                        }
                        override {
                            UnitedDetailsUiState.Content(
                                id = id,
                                detail = detail,
                                recommendations = recommendations
                            )
                        }
                    } catch (e: Exception) {
                        override { UnitedDetailsUiState.Error(e) }
                    }
                }
            }

            inState<UnitedDetailsUiState.Content> {
                onEnterEffect {
                    withContext(Dispatchers.IO) {
                        val now = Clock.System.now()

                        val id = snapshot.id
                        val detail = snapshot.detail
                        val title = detail.title

                        val newRecord = ReadingHistoryEntity(
                            id = id,
                            title = title,
                            author = detail.author,
                            coverUrl = detail.cover,
                            lastInteractionAt = now,
                            categories = detail.categories,
                            pagesCount = detail.pagesCount,
                            epsCount = detail.epsCount,
                            finished = detail.finished,
                            totalLikes = detail.totalLikes,
                            isFavourited = detail.isFavourited
                        )
                        historyDao.upsertHistory(newRecord)
                        Log.d(
                            TAG,
                            "Upsert history for '$title' with full comic details."
                        )
                    }
                }
                on<UnitedDetailsAction.ToggleLike> {
                    val currentDetail = snapshot.detail

                    try {
                        val r = network.toggleComicLike(snapshot.id)
                        val isLiked = when (r.action) {
                            ACTION_LIKE -> true
                            ACTION_UNLIKE -> false
                            else -> currentDetail.isLiked
                        }
                        mutate {
                            copy(detail = currentDetail.copy(isLiked = isLiked))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "ToggleLike: ", e)
                        noChange()
                    }
                }
                on<UnitedDetailsAction.ToggleFavorite> {
                    val currentDetail = snapshot.detail

                    try {
                        val r = network.toggleComicFavourite(snapshot.id)
                        val isFavourited = when (r.action) {
                            ACTION_FAVORITE -> true
                            ACTION_UN_FAVORITE -> false
                            else -> currentDetail.isFavourited
                        }
                        withContext(Dispatchers.IO) {
                            historyDao.updateIsFavourited(snapshot.id, isFavourited)
                            Log.d(TAG, "Sync isFavourited for '${snapshot.id}' to local database: $isFavourited")
                        }
                        mutate {
                            copy(detail = currentDetail.copy(isFavourited = isFavourited))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "ToggleFavorite: ", e)
                        noChange()
                    }
                }
                on<UnitedDetailsAction.ExpandReplies> {
                    mutate { copy(viewingRepliesForId = it.id) }
                }
                on<UnitedDetailsAction.CollapseReplies> {
                    mutate { copy(viewingRepliesForId = null) }
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
        fun create(id: String): UnitedDetailsStateMachine
    }

    private companion object {
        const val ACTION_LIKE = "like"
        const val ACTION_UNLIKE = "unlike"
        const val ACTION_FAVORITE = "favourite"
        const val ACTION_UN_FAVORITE = "un_favourite"
        private const val TAG = "UnitedDetailsStateMachine"
    }
}