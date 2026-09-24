package com.shizq.bika.feature.reader.impl

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.model.ChapterNavigation
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.model.reader.ReadingMode
import com.shizq.bika.core.ui.FullScreenLoading
import com.shizq.bika.feature.reader.impl.autoscroll.AutoScrollControlPanel
import com.shizq.bika.feature.reader.impl.autoscroll.AutoScrollSpeedRange
import com.shizq.bika.feature.reader.impl.autoscroll.rememberAutoScrollState
import com.shizq.bika.feature.reader.impl.bar.ReaderBottomBar
import com.shizq.bika.feature.reader.impl.bar.TopBar
import com.shizq.bika.feature.reader.impl.components.ChapterList
import com.shizq.bika.feature.reader.impl.components.ReadingModeSelectBottomSheet
import com.shizq.bika.feature.reader.impl.components.ReadingSettingsBottomSheet
import com.shizq.bika.feature.reader.impl.components.ScreenOrientationSelectBottomSheet
import com.shizq.bika.feature.reader.impl.components.ScrubPreviewOverlay
import com.shizq.bika.feature.reader.impl.components.StatusBarCapsule
import com.shizq.bika.feature.reader.impl.gesture.rememberGestureState
import com.shizq.bika.feature.reader.impl.layout.ChapterAppendRetryEffect
import com.shizq.bika.feature.reader.impl.layout.ReaderConfig
import com.shizq.bika.feature.reader.impl.layout.ReaderController
import com.shizq.bika.feature.reader.impl.layout.ReaderLayoutHost
import com.shizq.bika.feature.reader.impl.layout.SideSheetLayout
import com.shizq.bika.feature.reader.impl.layout.positionFlow
import com.shizq.bika.feature.reader.impl.layout.rememberReaderContext
import com.shizq.bika.feature.reader.impl.progress.ChapterKey
import com.shizq.bika.feature.reader.impl.progress.ReadingProgressEffect
import com.shizq.bika.feature.reader.impl.progress.ReadingProgressManager
import com.shizq.bika.feature.reader.impl.state.ReaderAction
import com.shizq.bika.feature.reader.impl.state.ReaderAction.HideSheet
import com.shizq.bika.feature.reader.impl.state.ReaderAction.JumpToChapter
import com.shizq.bika.feature.reader.impl.state.ReaderAction.SetAutoScrollSpeed
import com.shizq.bika.feature.reader.impl.state.ReaderAction.SetOrientation
import com.shizq.bika.feature.reader.impl.state.ReaderAction.SetReadingMode
import com.shizq.bika.feature.reader.impl.state.ReaderAction.ShowSheet
import com.shizq.bika.feature.reader.impl.state.ReaderAction.ToggleBarsVisibility
import com.shizq.bika.feature.reader.impl.state.ReaderSheet
import com.shizq.bika.feature.reader.impl.state.ReaderUiState
import com.shizq.bika.feature.reader.impl.system.ReaderSystemEffects
import com.shizq.bika.feature.reader.impl.util.ChapterAdvancePolicy
import com.shizq.bika.feature.reader.impl.util.ScrubState
import com.shizq.bika.feature.reader.impl.util.preload.ChapterPagePreloadProvider
import com.shizq.bika.feature.reader.impl.util.preload.PagingPreload
import com.shizq.bika.feature.reader.impl.util.preload.rememberAdaptivePreloadCount
import com.shizq.bika.feature.reader.impl.util.rememberScrubState
import com.shizq.bika.feature.reader.impl.util.rememberTopEndSystemAwarePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(viewModel: ReaderViewModel = hiltViewModel(), onBackClick: () -> Unit) {
    val uiState by viewModel.stateFlow.collectAsStateWithLifecycle()

    val pageItems = viewModel.imageListFlow.collectAsLazyPagingItems()
    val chapterItems = viewModel.chapterListFlow.collectAsLazyPagingItems()

    ReaderContent(
        state = uiState,
        pageItems = pageItems,
        chapterItems = chapterItems,
        progressManager = viewModel.progressManager,
        onBackClick = onBackClick,
        dispatch = viewModel::dispatch,
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(
    state: ReaderUiState,
    pageItems: LazyPagingItems<ChapterPage>,
    chapterItems: LazyPagingItems<Chapter>,
    progressManager: ReadingProgressManager,
    onBackClick: () -> Unit = {},
    dispatch: (ReaderAction) -> Unit = {},
) {
    when (state) {
        is ReaderUiState.Initializing -> FullScreenLoading()
        is ReaderUiState.Ready -> ReaderReadyContent(
            state = state,
            pageItems = pageItems,
            chapterItems = chapterItems,
            progressManager = progressManager,
            onBackClick = onBackClick,
            dispatch = dispatch,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderReadyContent(
    state: ReaderUiState.Ready,
    pageItems: LazyPagingItems<ChapterPage>,
    chapterItems: LazyPagingItems<Chapter>,
    progressManager: ReadingProgressManager,
    onBackClick: () -> Unit,
    dispatch: (ReaderAction) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val config = state.config
    val chapterState = state.chapter
    val overlayState = state.uiControl

    val readerContext = rememberReaderContext(
        readingMode = config.readingMode,
        chapterPages = pageItems,
        config = config,
        initialPageIndex = chapterState.initialPage,
        chapterOrder = chapterState.order,
    )
    val controller = readerContext.controller

    // 恢复 + 跟踪。单一 key（ChapterKey），恢复与跟踪在同一个协程里顺序执行。
    // 旧实现是三个 key 各走各路：LaunchedEffect(initialPage) 管恢复、
    // LaunchedEffect(manager) 管跟踪、controller 按 chapterOrder 重建——
    // 切到 initialPage 相同的章节时前两者不重启，跟踪协程会继续 collect 旧 controller。
    ReadingProgressEffect(
        manager = progressManager,
        chapterKey = ChapterKey(comicId = state.id, chapterOrder = chapterState.order),
        controller = controller,
        pageItems = pageItems,
        initialPage = chapterState.initialPage,
        totalPages = chapterState.totalPages,
        chapterTitle = chapterState.meta?.title.orEmpty(),
    )

    // 上下章导航：由 StateMachine 根据完整目录（state.catalog）解析出相邻章节，
    // 不再在 UI 层用 chapterList.peek() 推算——分页窗口只加载了首屏，
    // 当前章不在窗口内时 peek() 会永久找不到相邻章节。
    val navigation = state.navigation
    val hasNextChapter = navigation.next != null

    val autoScroll = rememberAutoScrollState(
        scroller = controller.continuousScroller,
        enabled = config.autoScrollEnabled,
        speed = config.autoScrollSpeed,
        hasNextChapter = hasNextChapter,
        onStop = {},
    )

    // 当前位置。直接读 controller 的快照状态，不再 collect 一个冷流——
    // 之前这里、章节自动衔接、进度跟踪、恢复确认各 collect 一次同一个冷
    // snapshotFlow，等于四个协程各跑一遍位置计算。
    // 分页退避重试只在这里驱动一次。放在占位项里会变成「每个可见占位项一条重试循环」。
    ChapterAppendRetryEffect(pageItems)

    val position = controller.position
    val currentPage = position.forProgress

    ReaderSystemEffects(
        showSystemBars = overlayState.showSystemBars,
        screenOrientation = config.screenOrientation,
    )
    ReaderBottomSheet(overlayState.readerSheet, config, dispatch)

    // TODO: 暂时移除
//            BackHandler(onBack = onBackClick)

    ChapterAutoAdvanceEffect(
        chapterOrder = chapterState.order,
        totalPages = chapterState.totalPages,
        controller = controller,
        navigation = navigation,
        onAdvance = { nextChapter, page ->
            dispatch(JumpToChapter(nextChapter, startFromBeginning = true, currentPage = page))
        },
        // todo 替换成 MessageReporter
        onNoMoreContent = {
            Toast.makeText(context, ReaderScreenMessages.NoMoreContent, Toast.LENGTH_SHORT).show()
        },
    )

    val preloadCount = rememberAdaptivePreloadCount(
        currentPage = currentPage,
        baselineCount = config.preloadCount,
    )

    val scrubState = rememberScrubState(chapterState.initialPage)
    LaunchedEffect(currentPage) { scrubState.syncToPage(currentPage) }

    val preloadModelProvider = remember(context) { ChapterPagePreloadProvider(context) }
    PagingPreload(
        pagingItems = pageItems,
        scrollStateProvider = readerContext.scrollStateProvider,
        modelProvider = preloadModelProvider,
        preloadCount = preloadCount
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (config.eyeCareEnabled) {
                    drawRect(Color.Black.copy(alpha = config.eyeCareDarkness))
                }
            }
    ) {
        ReaderScaffold(
            showMenu = overlayState.showSystemBars,
            topBar = {
                val title = chapterState.meta?.title ?: "Chapter ${chapterState.order}"
                TopBar(title = { Text(title) }, onBackClick = onBackClick)
            },
            bottomBar = {
                ReaderBottomBarSection(
                    currentPage = currentPage,
                    totalPages = chapterState.totalPages,
                    readingMode = config.readingMode,
                    navigation = navigation,
                    dispatch = dispatch,
                    scrubState = scrubState,
                    onSeekToPage = { scope.launch { controller.scrollToPage(it) } },
                    onNoMoreContent = {
                        Toast.makeText(
                            context,
                            ReaderScreenMessages.NoMoreContent,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                )
            },
            floatingMessage = {
                if (chapterState.totalPages > 0) {
                    // 复用已提升到本层的 position：原先 CurrentPageBadge 自己
                    // 又 collect 了一次页码流，同一个 Flow 被订阅两次。
                    // 跨页模式一屏两页，显示范围而不是只显示起始页——只显示起始页时
                    // 末屏永远停在「9 / 10」，用户以为还有一页没读。
                    PageIndicatorBadge(
                        pageNumber = position.first + 1,
                        lastPageNumber = position.last + 1,
                        total = chapterState.totalPages,
                    )
                }
            },
            sideSheet = {
                ReaderChapterListSheet(
                    visible = overlayState.readerSheet is ReaderSheet.ChapterList,
                    chapterItems = chapterItems,
                    currentChapterOrder = chapterState.order,
                    currentPage = currentPage,
                    dispatch = dispatch,
                )
            },
            content = {
                val gestureState = rememberGestureState(
                    layout = config.tapZoneLayout,
                    isRtl = config.readingMode.isRtl,
                )
                ReaderLayoutHost(
                    readerContext = readerContext,
                    gestureState = gestureState,
                    pageItems = pageItems,
                    toggleMenuVisibility = { dispatch(ToggleBarsVisibility) },
                    onHideMenu = {
                        if (overlayState.showSystemBars) {
                            dispatch(ToggleBarsVisibility)
                        }
                    }
                )
            }
        )

        // 只在支持连续滚动的模式下展示：Pager 模式下点了不会有任何反应
        if (config.autoScrollEnabled && autoScroll.isSupported) {
            AutoScrollControlPanel(
                isScrolling = autoScroll.isRunning,
                speed = config.autoScrollSpeed,
                onPlayPauseToggle = { autoScroll.togglePlayPause() },
                onSpeedUp = {
                    if (config.autoScrollSpeed < AutoScrollSpeedRange.last) {
                        dispatch(SetAutoScrollSpeed(config.autoScrollSpeed + 1))
                    }
                },
                onSpeedDown = {
                    if (config.autoScrollSpeed > AutoScrollSpeedRange.first) {
                        dispatch(SetAutoScrollSpeed(config.autoScrollSpeed - 1))
                    }
                },
                onClose = {
                    autoScroll.close()
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        if (config.statusBarCapsuleEnabled && !overlayState.showSystemBars) {
            val padding = rememberTopEndSystemAwarePadding(
                includeStatusBarInset = false,
                extraTop = 2.dp,
                extraEnd = 2.dp
            )
            StatusBarCapsule(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = padding.top,
                        end = padding.end
                    )
            )
        }

        ScrubPreviewOverlay(
            scrubState = scrubState,
            pageItems = pageItems,
            totalPages = chapterState.totalPages,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

private object ReaderScreenMessages {
    const val NoMoreContent = "后面没有内容了"
}

/**
 * 章节自动衔接：到达当前章节最后一页时自动跳转到下一章；已是最后一章则回调 [onNoMoreContent]。
 *
 * 用 totalPages 作为 key 而非 snapshotFlow { totalPages }：totalPages 不是 Compose State，
 * snapshotFlow 无依赖可订阅，初始为 0 时 first() 会永久挂起。改为 key 后，totalPages 从 0
 * 变为非零值会触发 recomposition 重启这个 effect，天然实现“等待章节加载完成后再监听”。
 */
@Composable
private fun ChapterAutoAdvanceEffect(
    chapterOrder: Int,
    totalPages: Int,
    controller: ReaderController,
    navigation: ChapterNavigation,
    onAdvance: (nextChapter: Chapter, page: Int) -> Unit,
    onNoMoreContent: () -> Unit,
    policy: ChapterAdvancePolicy = remember { ChapterAdvancePolicy() },
) {
    val nextChapter = navigation.next
    LaunchedEffect(chapterOrder, totalPages, nextChapter, controller) {
        if (totalPages <= 0) return@LaunchedEffect
        // 末页判定必须用 forEndOfChapter（当前屏的**最后**一页）。
        //
        // 用起始页会让跨页模式永远读不完一章：10 页分成 D(0,1)…D(8,9)，末屏的
        // 起始页恒为 8，isAtLastPage(8, 10) 判 8 >= 9 为假 —— 自动衔接不触发、
        // 「已读完」标记拿不到、页码徽章停在 9/10。
        controller.positionFlow()
            .map { it.forEndOfChapter }
            .distinctUntilChanged()
            .debounce(policy.endOfChapterDebounce)
            .collect { page ->
                if (policy.isAtLastPage(page, totalPages)) {
                    delay(policy.advanceDelay)
                    if (nextChapter != null) {
                        // 自动跳转下一章，从头开始阅读，不恢复该章历史进度
                        onAdvance(nextChapter, page)
                    } else {
                        onNoMoreContent()
                    }
                }
            }
    }
}

@Composable
private fun ReaderBottomBarSection(
    currentPage: Int,
    totalPages: Int,
    readingMode: ReadingMode,
    navigation: ChapterNavigation,
    dispatch: (ReaderAction) -> Unit,
    scrubState: ScrubState,
    onSeekToPage: (Int) -> Unit,
    onNoMoreContent: () -> Unit,
) {
    val prevChapter = navigation.prev
    val nextChapter = navigation.next

    ReaderBottomBar(
        currentPage = currentPage,
        totalPages = totalPages,
        readingMode = readingMode,
        onSeekToPage = onSeekToPage,
        onToggleChapterList = { dispatch(ShowSheet(ReaderSheet.ChapterList)) },
        onOpenSettings = { dispatch(ShowSheet(ReaderSheet.Settings)) },
        onOpenReadingMode = { dispatch(ShowSheet(ReaderSheet.ReadingMode)) },
        onOpenOrientation = { dispatch(ShowSheet(ReaderSheet.Orientation)) },
        hasPrevChapter = prevChapter != null,
        hasNextChapter = nextChapter != null,
        onPrevChapter = {
            prevChapter?.let {
                dispatch(JumpToChapter(it, currentPage = currentPage))
            }
        },
        onNextChapter = {
            if (nextChapter != null) {
                dispatch(JumpToChapter(nextChapter, currentPage = currentPage))
            } else {
                onNoMoreContent()
            }
        },
        scrubState = scrubState,
    )
}

@Composable
private fun ReaderChapterListSheet(
    visible: Boolean,
    chapterItems: LazyPagingItems<Chapter>,
    currentChapterOrder: Int,
    currentPage: Int,
    dispatch: (ReaderAction) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            animationSpec = tween(),
            initialOffsetX = { -it }
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(),
            targetOffsetX = { -it }
        ),
    ) {
        SideSheetLayout(
            title = { Text("目录") },
            onDismissRequest = { dispatch(HideSheet) },
            closeButton = {
                IconButton(onClick = { dispatch(HideSheet) }) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "关闭目录"
                    )
                }
            }
        ) {
            ChapterList(
                chapters = chapterItems,
                currentChapterOrder = currentChapterOrder,
                onChapterClick = { newChapter ->
                    dispatch(JumpToChapter(newChapter, currentPage = currentPage))
                },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * @param pageNumber 当前屏起始页，1-based（即 index + 1）
 * @param lastPageNumber 当前屏末页，1-based。与 [pageNumber] 相等时只显示一个数字。
 *   跨页模式一屏两页，显示 "9-10 / 10" 而不是 "9 / 10"——后者会让用户以为
 *   最后一页没读到。
 */
@Composable
fun PageIndicatorBadge(pageNumber: Int, total: Int, lastPageNumber: Int = pageNumber) {
    val label = if (lastPageNumber > pageNumber) {
        "$pageNumber-$lastPageNumber / $total"
    } else {
        "$pageNumber / $total"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderBottomSheet(
    sheet: ReaderSheet,
    config: ReaderConfig,
    dispatch: (ReaderAction) -> Unit
) {
    val onClose = { dispatch(HideSheet) }
    when (sheet) {
        ReaderSheet.ReadingMode -> {
            ReadingModeSelectBottomSheet(
                activeMode = config.readingMode,
                onReadingModeChanged = {
                    dispatch(SetReadingMode(it))
                },
                onDismissRequest = onClose
            )
        }

        ReaderSheet.Orientation -> {
            ScreenOrientationSelectBottomSheet(
                orientation = config.screenOrientation,
                onOrientationChange = { dispatch(SetOrientation(it)) },
                onDismissRequest = onClose
            )
        }

        ReaderSheet.Settings -> {
            ReadingSettingsBottomSheet(
                config = config,
                dispatch = dispatch,
                onDismissRequest = onClose,
            )
        }

        else -> {}
    }
}


