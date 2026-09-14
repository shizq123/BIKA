@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.shizq.bika.feature.reader.impl.statemachine

import androidx.lifecycle.SavedStateHandle
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.database.dao.ReadingHistoryDao
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.feature.reader.impl.layout.ReaderConfig
import com.shizq.bika.feature.reader.impl.progress.ChapterProgress
import com.shizq.bika.feature.reader.impl.progress.ProgressWriteCoordinator
import com.shizq.bika.feature.reader.impl.state.ChapterState
import com.shizq.bika.feature.reader.impl.state.ReaderAction
import com.shizq.bika.feature.reader.impl.state.ReaderSheet
import com.shizq.bika.feature.reader.impl.state.ReaderUiState
import com.shizq.bika.feature.reader.impl.state.UiControlState
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger("ReaderProgress")

class ReaderStateMachine @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val historyDao: ReadingHistoryDao,
) : FlowReduxStateMachineFactory<ReaderUiState, ReaderAction>() {

    /**
     * 由 ReaderViewModel 在创建 ReadingProgressManager 后接上。
     * 状态机自身不持有 ReadingProgressStore——进度写入只有一个入口（写入流水线），
     * 状态机直接写库正是旧实现里第四个无序写入者的来源。
     */
    var progressWriteCoordinator: ProgressWriteCoordinator = ProgressWriteCoordinator.NoOp

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
                on<ReaderAction.JumpToChapter> { action ->
                    val previousChapter = snapshot.chapter
                    val newOrder = action.chapter.order
                    savedStateHandle["order"] = newOrder

                    // 用跳转前的 snapshot 构造旧章节进度，交给写入流水线。
                    // 与切换章节合并成同一次 dispatch，天然保证顺序正确。
                    //
                    // 改动点：不再直接调 progressStore.store。那是第四个独立写入者，
                    // 与防抖写入之间没有顺序约束。现在走 progressManager，与其余
                    // 三条路径共用同一个 collector；同时它会关闭写库闸门，
                    // 让新章节在恢复确认前不写库。
                    progressWriteCoordinator.onChapterSwitch(
                        ChapterProgress(
                            comicId = snapshot.id,
                            chapterOrder = previousChapter.order,
                            pageIndex = action.currentPage,
                            totalPages = previousChapter.meta?.totalImages ?: 0,
                            chapterTitle = previousChapter.meta?.title.orEmpty(),
                        )
                    )

                    // startFromBeginning=true：自动跳转到下一章，始终从第 0 页开始。
                    // startFromBeginning=false（默认）：手动跳章，恢复该章节上次阅读位置。
                    val startPage = if (action.startFromBeginning) 0 else getStartPage(snapshot.id, newOrder)
                    mutate {
                        val newChapterState = ChapterState(
                            order = newOrder,
                            isLoading = true,
                            initialPage = startPage,
                        )
                        // uiControl 整体重置：切章后浮层应关闭、栏显隐回到默认。
                        copy(
                            chapter = newChapterState,
                            uiControl = UiControlState(),
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
                on<ReaderAction.ChapterCatalogLoaded> {
                    mutate { copy(catalog = it.catalog) }
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
                // PersistProgress action 已删除：进度写入不再绕状态机一圈。
                // 旧路径是 controller 观测到页码 -> onPersist 回调 -> dispatch(PersistProgress)
                // -> handler 从 snapshot 读章节信息 -> store。中间那次 dispatch 让
                // 「写哪一章」取决于 action 被调度的时刻，切章瞬间会写错章节。
                // 现在页码在观测点就与章节身份绑成 ChapterProgress，直接进写入流水线。
            }
        }
    }

    private suspend fun getStartPage(historyId: String, chapterOrder: Int): Int {
        return withContext(Dispatchers.IO) {
            val history = historyDao.getDetailedHistoryById(historyId)

            val progress =
                history?.asExternalModel()?.progressList?.find { it.chapterNumber == chapterOrder }
            val startPage = progress?.currentPage ?: 0
            logger.debug { "恢复进度: comic=$historyId 章节=$chapterOrder 找到进度=${progress != null} DB进度=${progress?.currentPage}/${progress?.pageCount} 起始页=$startPage" }
            startPage
        }
    }
}