@file:OptIn(ExperimentalCoroutinesApi::class)

package com.shizq.bika.ui.reader.statemachine

import androidx.lifecycle.SavedStateHandle
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.ReadingHistoryEntity
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.ui.reader.layout.ReaderConfig
import com.shizq.bika.ui.reader.state.ChapterState
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.state.ReaderSheet
import com.shizq.bika.ui.reader.state.ReaderUiState
import com.shizq.bika.ui.reader.state.SeekState
import com.shizq.bika.ui.reader.state.UiControlState
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class ReaderStateMachine @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val historyDao: ReadingHistoryDao,
    private val downloadTaskRepository: DownloadTaskRepository,
    @ApplicationScope private val externalScope: CoroutineScope,
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

                    val id = snapshot.id
                    if (id.isNotEmpty() && meta != null) {
                        val pageIndex = it.pageIndex
                        externalScope.launch(Dispatchers.IO) {
                            val now = Clock.System.now()
                            val affectedRows = historyDao.updateLastReadAt(id, now)
                            if (affectedRows == 0) {
                                // 历史条目不存在（如离线直接打开下载章节），为避免外键冲突，先插入默认漫画主历史记录
                                val task =
                                    downloadTaskRepository.observeTask("${id}_${chapter.order}")
                                        .first()
                                val title = task?.comicTitle ?: meta.title.ifEmpty { "Comic $id" }
                                val coverUrl = task?.coverUrl ?: ""
                                val newRecord = ReadingHistoryEntity(
                                    id = id,
                                    title = title,
                                    author = "未知作者",
                                    coverUrl = coverUrl,
                                    lastInteractionAt = now
                                )
                                historyDao.upsertHistory(newRecord)
                            }

                            // 如果翻到最后一页或最后2页，则直接保存当前页为总页数，反馈已经看完
                            val isFinished = meta.totalImages > 0 && pageIndex >= meta.totalImages - 2
                            val savedPage = if (isFinished) meta.totalImages else pageIndex

                            val chapterProgress = ChapterProgressEntity(
                                historyId = id,
                                chapterId = chapter.order,
                                currentPage = savedPage,
                                pageCount = meta.totalImages,
                                lastReadAt = now
                            )
                            historyDao.upsertChapterProgress(chapterProgress)

                            // 如果看完，则同步将该章节的下载任务标记为已查看
                            if (isFinished) {
                                val taskId = "${id}_${chapter.order}"
                                downloadTaskRepository.markAsViewed(taskId)
                            }
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
                        volumeKeyNavigation = it.volumeKeyNavigation,
                        readingMode = it.readingMode,
                        screenOrientation = it.screenOrientation,
                        tapZoneLayout = it.tapZoneLayout,
                        preloadCount = it.preloadCount,
                        eyeCareEnabled = it.eyeCareEnabled,
                        eyeCareDarkness = it.eyeCareDarkness,
                        autoScrollEnabled = it.autoScrollEnabled,
                        autoScrollSpeed = it.autoScrollSpeed,
                        bookSpreadsMode = it.bookSpreadsMode,
                        magnifierEnabled = it.magnifierEnabled,
                        statusBarCapsuleEnabled = it.statusBarCapsuleEnabled,
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