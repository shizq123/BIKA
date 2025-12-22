package com.shizq.bika.ui.reader.statemachine

import androidx.lifecycle.SavedStateHandle
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.ui.reader.ReaderActivity.Companion.EXTRA_ORDER
import com.shizq.bika.ui.reader.layout.ReaderConfig
import com.shizq.bika.ui.reader.state.ChapterState
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.state.ReaderSheet
import com.shizq.bika.ui.reader.state.ReaderUiState
import com.shizq.bika.ui.reader.state.SeekState
import com.shizq.bika.ui.reader.state.UiControlState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class ReaderStateMachine @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val historyDao: ReadingHistoryDao,
    @Assisted private val id: String,
    @Assisted private val startOrder: Int,
) : FlowReduxStateMachineFactory<ReaderUiState, ReaderAction>() {
    init {
        initializeWith { ReaderUiState.Initializing }
        spec {
            inState<ReaderUiState.Initializing> {
                onEnter {
                    override {
                        ReaderUiState.Ready(
                            id = id,
                            chapter = ChapterState(startOrder, isLoading = true)
                        )
                    }
                }
            }
            inState<ReaderUiState.Ready> {
                on<ReaderAction.JumpToChapter> {
                    savedStateHandle[EXTRA_ORDER] = it.chapter.order

                    mutate {
                        val newChapterState = ChapterState(
                            order = it.chapter.order,
                            meta = null,
                            totalPages = 0,
                            isLoading = true
                        )
                        copy(
                            chapter = newChapterState,
                            uiControl = UiControlState(
                                seekState = SeekState.Idle
                            )
                        )
                    }
                }
                on<ReaderAction.ChapterMetaLoaded> {
                    mutate {
                        copy(
                            chapter = chapter.copy(
                                meta = it.meta,
                                totalPages = it.meta.totalImages,
                                isLoading = false
                            )
                        )
                    }
                }
                on<ReaderAction.Initialize> {
                    val page = withContext(Dispatchers.IO) {
                        val history = historyDao.getDetailedHistoryById(snapshot.id)
                        val targetProgress = history?.asExternalModel()?.progressList?.find {
                            it.chapterNumber == snapshot.chapter.order
                        }
                        targetProgress?.currentPage ?: 0
                    }
                    if (page > 0) {
                        mutate {
                            copy(
                                uiControl = uiControl.copy(
                                    seekState = SeekState.Seeking(page.toFloat())
                                )
                            )
                        }
                    } else {
                        noChange()
                    }
                }
                onActionEffect<ReaderAction.SyncReadingProgress> {
                    val chapter = snapshot.chapter
                    val meta = chapter.meta

                    val id = snapshot.id
                    if (id.isNotEmpty() && meta != null) {
                        withContext(Dispatchers.IO) {
                            val now = Clock.System.now()
                            historyDao.updateLastReadAt(id, now)

                            val chapterProgress = ChapterProgressEntity(
                                historyId = id,
                                chapterId = chapter.order,
                                currentPage = it.pageIndex,
                                pageCount = meta.totalImages,
                                lastReadAt = now
                            )
                            historyDao.upsertChapterProgress(chapterProgress)
                        }
                    }
                }
                onActionEffect<ReaderAction.SetReadingMode> {
                    userPreferencesDataSource.setReadingMode(it.mode)
                }
                onActionEffect<ReaderAction.SetOrientation> {
                    userPreferencesDataSource.setScreenOrientation(it.orientation)
                }
                onActionEffect<ReaderAction.SetPreloadCount> {
                    userPreferencesDataSource.setPreloadCount(it.count)
                }
                onActionEffect<ReaderAction.SetTapZoneLayout> {
                    userPreferencesDataSource.setTapZoneLayout(it.layout)
                }
                onActionEffect<ReaderAction.SetVolumeKeyNavigation> {
                    userPreferencesDataSource.setIsVolumeKeyNavigation(it.enable)
                }
                collectWhileInState(userPreferencesDataSource.userData) {
                    val newConfig = ReaderConfig(
                        volumeKeyNavigation = it.volumeKeyNavigation,
                        readingMode = it.readingMode,
                        screenOrientation = it.screenOrientation,
                        tapZoneLayout = it.tapZoneLayout,
                        preloadCount = it.preloadCount
                    )
                    mutate { copy(config = newConfig) }
                }

                on<ReaderAction.ToggleBarsVisibility> {
                    mutate {
                        copy(uiControl = uiControl.copy(showSystemBars = !uiControl.showSystemBars))
                    }
                }
                on<ReaderAction.ShowSheet> {
                    mutate {
                        copy(uiControl = uiControl.copy(readerSheet = it.sheet))
                    }
                }
                on<ReaderAction.HideSheet> {
                    mutate {
                        copy(uiControl = uiControl.copy(readerSheet = ReaderSheet.None))
                    }
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        operator fun invoke(id: String, order: Int): ReaderStateMachine
    }
}