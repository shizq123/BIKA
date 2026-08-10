@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.reader.statemachine

import androidx.lifecycle.SavedStateHandle
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.ui.reader.ReadingProgressSaver
import com.shizq.bika.ui.reader.layout.ReaderConfig
import com.shizq.bika.ui.reader.state.ChapterState
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.state.ReaderSheet
import com.shizq.bika.ui.reader.state.ReaderUiState
import com.shizq.bika.ui.reader.state.SeekState
import com.shizq.bika.ui.reader.state.UiControlState
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

class ReaderStateMachine @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val historyDao: ReadingHistoryDao,
    private val progressSaver: ReadingProgressSaver,
) : FlowReduxStateMachineFactory<ReaderUiState, ReaderAction>() {
    init {
        spec {
            inState<ReaderUiState.Initializing> {
                onEnter {
                    val startPage = getStartPage(snapshot.id, snapshot.order)

                    override {
                        ReaderUiState.Ready(
                            id = id,
                            chapter = ChapterState(
                                order = order,
                                initialPage = startPage,
                                isLoading = true
                            )
                        )
                    }
                }
            }
            inState<ReaderUiState.Ready> {
                on<ReaderAction.JumpToChapter> { chapter ->
                    val newOrder = chapter.chapter.order
                    savedStateHandle["order"] = newOrder

                    // startFromBeginning=true：自动跳转到下一章，始终从第 0 页开始。
                    // startFromBeginning=false（默认）：手动跳章，恢复该章节上次阅读位置。
                    val startPage = if (chapter.startFromBeginning) 0 else getStartPage(snapshot.id, newOrder)
                    mutate {
                        val newChapterState = ChapterState(
                            order = newOrder,
                            isLoading = true,
                            initialPage = startPage,
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
                onActionEffect<ReaderAction.SyncReadingProgress> {
                    val chapter = snapshot.chapter
                    val meta = chapter.meta
                    progressSaver.save(snapshot.id, chapter.order, meta, it.pageIndex)
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
                onActionEffect<ReaderAction.SetEyeCareEnabled> {
                    userPreferencesDataSource.setEyeCareEnabled(it.enable)
                }
                onActionEffect<ReaderAction.SetEyeCareDarkness> {
                    userPreferencesDataSource.setEyeCareDarkness(it.darkness)
                }
                onActionEffect<ReaderAction.SetAutoScrollEnabled> {
                    userPreferencesDataSource.setAutoScrollEnabled(it.enable)
                }
                onActionEffect<ReaderAction.SetAutoScrollSpeed> {
                    userPreferencesDataSource.setAutoScrollSpeed(it.speed)
                }
                onActionEffect<ReaderAction.SetBookSpreadsMode> {
                    userPreferencesDataSource.setBookSpreadsMode(it.mode)
                }
                onActionEffect<ReaderAction.SetMagnifierEnabled> {
                    userPreferencesDataSource.setMagnifierEnabled(it.enable)
                }
                onActionEffect<ReaderAction.SetStatusBarCapsuleEnabled> {
                    userPreferencesDataSource.setStatusBarCapsuleEnabled(it.enable)
                }
                collectWhileInState(userPreferencesDataSource.userData) {
                    val newConfig = ReaderConfig(
                        volumeKeyNavigation = it.reader.volumeKeyNavigationEnabled,
                        readingMode = it.reader.readingMode,
                        screenOrientation = it.reader.screenOrientation,
                        tapZoneLayout = it.reader.tapZoneLayout,
                        preloadCount = it.reader.preloadCount,
                        eyeCareEnabled = it.reader.eyeCare.enabled,
                        eyeCareDarkness = it.reader.eyeCare.darkness,
                        autoScrollEnabled = it.reader.autoScroll.enabled,
                        autoScrollSpeed = it.reader.autoScroll.speed,
                        bookSpreadsMode = it.reader.bookSpreadsMode,
                        magnifierEnabled = it.reader.magnifierEnabled,
                        statusBarCapsuleEnabled = it.reader.statusBarCapsuleEnabled,
                    )
                    mutate { copy(config = newConfig) }
                }

                on<ReaderAction.ToggleBarsVisibility> {
                    mutate {
                        copy(uiControl = uiControl.copy(showSystemBars = !uiControl.showSystemBars))
                    }
                }
                on<ReaderAction.SetBarsVisibility> {
                    mutate {
                        copy(uiControl = uiControl.copy(showSystemBars = it.visible))
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
                on<ReaderAction.SeekConsumed> {
                    mutate {
                        copy(uiControl = uiControl.copy(seekState = SeekState.Idle))
                    }
                }
            }
        }
    }

    private suspend fun getStartPage(historyId: String, chapterOrder: Int): Int {
        return withContext(Dispatchers.IO) {
            val history = historyDao.getDetailedHistoryById(historyId) ?: return@withContext 0
            history.asExternalModel().progressList
                .find { it.chapterNumber == chapterOrder }
                ?.let { progress ->
                    if (progress.currentPage >= progress.pageCount && progress.pageCount > 0) {
                        0
                    } else {
                        progress.currentPage
                    }
                } ?: 0
        }
    }
}