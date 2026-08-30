package com.shizq.bika.feature.reader.impl

import android.content.pm.ActivityInfo
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.shizq.bika.core.common.BikaLog
import com.shizq.bika.core.context.findActivity
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.model.ChapterNavigation
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.model.reader.ReadingMode
import com.shizq.bika.core.model.reader.ScreenOrientation
import com.shizq.bika.core.ui.FullScreenLoading
import com.shizq.bika.core.ui.composition.LocalWindow
import com.shizq.bika.feature.reader.impl.autoscroll.AutoScrollControlPanel
import com.shizq.bika.feature.reader.impl.autoscroll.rememberAutoScrollController
import com.shizq.bika.feature.reader.impl.bar.ReaderBottomBar
import com.shizq.bika.feature.reader.impl.bar.TopBar
import com.shizq.bika.feature.reader.impl.components.ChapterList
import com.shizq.bika.feature.reader.impl.components.ReadingModeSelectBottomSheet
import com.shizq.bika.feature.reader.impl.components.ReadingSettingsBottomSheet
import com.shizq.bika.feature.reader.impl.components.ScreenOrientationSelectBottomSheet
import com.shizq.bika.feature.reader.impl.components.StatusBarCapsule
import com.shizq.bika.feature.reader.impl.gesture.rememberGestureState
import com.shizq.bika.feature.reader.impl.layout.LocalReaderConfig
import com.shizq.bika.feature.reader.impl.layout.ReaderConfig
import com.shizq.bika.feature.reader.impl.layout.ReaderController
import com.shizq.bika.feature.reader.impl.layout.ReaderLayoutHost
import com.shizq.bika.feature.reader.impl.layout.SideSheetLayout
import com.shizq.bika.feature.reader.impl.layout.rememberReaderContext
import com.shizq.bika.feature.reader.impl.progress.ProgressState
import com.shizq.bika.feature.reader.impl.progress.rememberReadingProgressManager
import com.shizq.bika.feature.reader.impl.state.ReaderAction
import com.shizq.bika.feature.reader.impl.state.ReaderAction.HideSheet
import com.shizq.bika.feature.reader.impl.state.ReaderAction.JumpToChapter
import com.shizq.bika.feature.reader.impl.state.ReaderAction.SetAutoScrollEnabled
import com.shizq.bika.feature.reader.impl.state.ReaderAction.SetAutoScrollSpeed
import com.shizq.bika.feature.reader.impl.state.ReaderAction.SetOrientation
import com.shizq.bika.feature.reader.impl.state.ReaderAction.SetReadingMode
import com.shizq.bika.feature.reader.impl.state.ReaderAction.ShowSheet
import com.shizq.bika.feature.reader.impl.state.ReaderAction.ToggleBarsVisibility
import com.shizq.bika.feature.reader.impl.state.ReaderSheet
import com.shizq.bika.feature.reader.impl.state.ReaderUiState
import com.shizq.bika.feature.reader.impl.state.SeekState
import com.shizq.bika.feature.reader.impl.util.ChapterAdvancePolicy
import com.shizq.bika.feature.reader.impl.util.preload.AdaptivePreloadTracker
import com.shizq.bika.feature.reader.impl.util.preload.ChapterPagePreloadProvider
import com.shizq.bika.feature.reader.impl.util.preload.PagingPreload
import com.shizq.bika.feature.reader.impl.util.rememberTopEndSystemAwarePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
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
        onBackClick = onBackClick,
        dispatch = viewModel::dispatch,
        persistProgressBlocking = { viewModel.saveProgress(it) },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(
    state: ReaderUiState,
    pageItems: LazyPagingItems<ChapterPage>,
    chapterItems: LazyPagingItems<Chapter>,
    onBackClick: () -> Unit = {},
    dispatch: (ReaderAction) -> Unit = {},
    persistProgressBlocking: (Int) -> Boolean = { false },
) {
    when (state) {
        is ReaderUiState.Initializing -> FullScreenLoading()
        is ReaderUiState.Ready -> ReaderReadyContent(
            state = state,
            pageItems = pageItems,
            chapterItems = chapterItems,
            onBackClick = onBackClick,
            dispatch = dispatch,
            persistProgressBlocking = persistProgressBlocking,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderReadyContent(
    state: ReaderUiState.Ready,
    pageItems: LazyPagingItems<ChapterPage>,
    chapterItems: LazyPagingItems<Chapter>,
    onBackClick: () -> Unit,
    dispatch: (ReaderAction) -> Unit,
    persistProgressBlocking: (Int) -> Boolean,
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

    val progressManager = rememberReadingProgressManager(
        controller = controller,
        imageList = pageItems,
        initialPage = chapterState.initialPage,
        onPersist = persistProgressBlocking
    )

    // 监听进度恢复状态（用于调试和日志）
    val progressState by progressManager.state.collectAsStateWithLifecycle()
    LaunchedEffect(progressState) {
        when (val restoreState = progressState) {
            is ProgressState.Restoring -> {
                BikaLog.d("ReaderScreen", "正在恢复进度到第 ${restoreState.targetPage} 页")
            }

            is ProgressState.Restored -> {
                BikaLog.d("ReaderScreen", "进度已恢复到第 ${restoreState.actualPage} 页")
            }

            is ProgressState.RestoreFailed -> {
                BikaLog.w("ReaderScreen", "进度恢复失败: ${restoreState.reason}")
            }

            is ProgressState.Tracking -> {
                // 正在跟踪页面变化，不需要日志（太频繁）
            }

            else -> {}
        }
    }

    // 上下章导航：由 StateMachine 根据完整目录（state.catalog）解析出相邻章节，
    // 不再在 UI 层用 chapterList.peek() 推算——分页窗口只加载了首屏，
    // 当前章不在窗口内时 peek() 会永久找不到相邻章节。
    val navigation = state.navigation
    val hasNextChapter = navigation.next != null

    // 自动滚动依赖像素级连续滚动能力（controller.continuousScroller），
    // Pager 模式下该值为 null，AutoScrollController 会据此保持不可用状态。
    val autoScroll = rememberAutoScrollController(
        scroller = controller.continuousScroller,
        enabled = config.autoScrollEnabled,
        speed = config.autoScrollSpeed,
        hasNextChapter = hasNextChapter,
        onReachEnd = {
            Toast.makeText(context, ReaderScreenMessages.NoMoreContent, Toast.LENGTH_SHORT).show()
        },
        onSettingChanged = { dispatch(SetAutoScrollEnabled(it)) },
    )

    // 当前页（提升到此层级，用于自动衔接检测）
    val currentPage by controller.visibleItemIndex.collectAsState(0)

    ReaderSystemEffects(
        showSystemBars = overlayState.showSystemBars,
        screenOrientation = config.screenOrientation,
    )
    ReaderBottomSheet(overlayState.readerSheet, config, dispatch)

    // TODO: 暂时移除
//            BackHandler(onBack = onBackClick)

    LaunchedEffect(overlayState.seekState) {
        if (overlayState.seekState is SeekState.Seeking) {
            controller.scrollToPage(overlayState.seekState.targetPage.toInt())
            dispatch(ReaderAction.SeekConsumed)
        }
    }

    ChapterAutoAdvanceEffect(
        chapterOrder = chapterState.order,
        totalPages = chapterState.totalPages,
        visibleItemIndex = controller.visibleItemIndex,
        navigation = navigation,
        onAdvance = { nextChapter, page ->
            dispatch(JumpToChapter(nextChapter, startFromBeginning = true, currentPage = page))
        },
        onNoMoreContent = {
            Toast.makeText(context, ReaderScreenMessages.NoMoreContent, Toast.LENGTH_SHORT).show()
        },
    )

    val preloadCount = rememberAdaptivePreloadCount(
        currentPage = currentPage,
        baselineCount = config.preloadCount,
    )

    var draggedPage by remember { mutableStateOf<Int?>(null) }

    val preloadModelProvider = remember(context) { ChapterPagePreloadProvider(context) }
    PagingPreload(
        pagingItems = pageItems,
        scrollStateProvider = readerContext.scrollStateProvider,
        modelProvider = preloadModelProvider,
        preloadCount = preloadCount
    )

    CompositionLocalProvider(LocalReaderConfig provides config) {
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
                        onSeekToPage = { scope.launch { controller.scrollToPage(it) } },
                        onSeeking = { draggedPage = it },
                        onSeekingFinished = { draggedPage = null },
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
                        CurrentPageBadge(
                            controller = controller,
                            totalPages = chapterState.totalPages
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
                    isScrolling = autoScroll.isScrolling,
                    speed = config.autoScrollSpeed,
                    onPlayPauseToggle = { autoScroll.togglePause() },
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
                        autoScroll.disableAndPersist()
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

            draggedPage?.let { previewPage ->
                val pageUrl = if (previewPage in 0 until pageItems.itemCount) {
                    pageItems.peek(previewPage)?.url
                } else {
                    null
                }
                ScrubPreviewCard(
                    pageUrl = pageUrl,
                    previewPageIndex = previewPage,
                    totalPages = chapterState.totalPages,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/** 自动滚动速度可调范围（含端点），对应设置面板中的加速/减速按钮。 */
private val AutoScrollSpeedRange = 1..10

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
    visibleItemIndex: kotlinx.coroutines.flow.Flow<Int>,
    navigation: ChapterNavigation,
    onAdvance: (nextChapter: Chapter, page: Int) -> Unit,
    onNoMoreContent: () -> Unit,
    policy: ChapterAdvancePolicy = remember { ChapterAdvancePolicy() },
) {
    val nextChapter = navigation.next
    LaunchedEffect(chapterOrder, totalPages, nextChapter) {
        if (totalPages <= 0) return@LaunchedEffect
        // 监听页面到达末尾（停留一段时间确认用户确实看到最后一页）
        visibleItemIndex
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

/**
 * 根据翻页速率动态调整预载张数：扫读时提高预载数量，精读时降低，参见 [AdaptivePreloadTracker]。
 */
@Composable
private fun rememberAdaptivePreloadCount(currentPage: Int, baselineCount: Int): Int {
    val tracker = remember { AdaptivePreloadTracker() }
    var preloadCount by remember(baselineCount) { mutableIntStateOf(baselineCount) }

    LaunchedEffect(currentPage) {
        preloadCount = tracker.onPageChanged(System.currentTimeMillis(), baselineCount)
    }

    return preloadCount
}

@Composable
private fun ReaderBottomBarSection(
    currentPage: Int,
    totalPages: Int,
    readingMode: ReadingMode,
    navigation: ChapterNavigation,
    dispatch: (ReaderAction) -> Unit,
    onSeekToPage: (Int) -> Unit,
    onSeeking: (Int) -> Unit,
    onSeekingFinished: () -> Unit,
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
        onSeeking = onSeeking,
        onSeekingFinished = onSeekingFinished,
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
 * @param pageNumber 用于展示的页码，1-based（即 index + 1）
 */
@Composable
fun PageIndicatorBadge(pageNumber: Int, total: Int) {
    Text(
        text = "$pageNumber / $total",
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

/**
 * 阅读器所需的系统级副作用集合：屏幕方向锁定、常亮、系统栏显隐。
 */
@Composable
private fun ReaderSystemEffects(
    showSystemBars: Boolean,
    screenOrientation: ScreenOrientation,
) {
    SystemBarsEffect(showSystemBars = showSystemBars)
    KeepScreenOnEffect()
    OrientationEffect(screenOrientation)
}

@Composable
fun OrientationEffect(orientation: ScreenOrientation) {
    val context = LocalContext.current
    LaunchedEffect(orientation) {
        val activity = context.findActivity()
        activity?.requestedOrientation = when (orientation) {
            ScreenOrientation.System -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ScreenOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            ScreenOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ScreenOrientation.LockPortrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.LockLandscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ScreenOrientation.ReversePortrait -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        }
    }
}

@Composable
fun KeepScreenOnEffect() {
    val window = LocalWindow.current

    DisposableEffect(Unit) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
private fun SystemBarsEffect(showSystemBars: Boolean) {
    val window = LocalWindow.current

    DisposableEffect(window, showSystemBars) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (showSystemBars) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
private fun CurrentPageBadge(controller: ReaderController, totalPages: Int) {
    val currentPageIndex by controller.visibleItemIndex.collectAsState(0)
    PageIndicatorBadge(pageNumber = currentPageIndex + 1, total = totalPages)
}

/**
 * @param previewPageIndex 拖动进度条时预览的目标页（0-based），不代表当前实际停留的页
 */
@Composable
private fun ScrubPreviewCard(
    pageUrl: String?,
    previewPageIndex: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.75f),
        border = BorderStroke(
            0.5.dp,
            Color.White.copy(alpha = 0.15f)
        ),
        shadowElevation = 4.dp,
        modifier = modifier
            .width(90.dp)
            .height(130.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!pageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(pageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Preview Page ${previewPageIndex + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${previewPageIndex + 1} / $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
