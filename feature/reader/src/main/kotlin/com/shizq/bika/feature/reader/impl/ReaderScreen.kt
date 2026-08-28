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
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.model.reader.ReadingMode
import com.shizq.bika.core.model.reader.ScreenOrientation
import com.shizq.bika.core.ui.FullScreenLoading
import com.shizq.bika.core.ui.composition.LocalWindow
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
import com.shizq.bika.feature.reader.impl.util.preload.ChapterPagePreloadProvider
import com.shizq.bika.feature.reader.impl.util.preload.PagingPreload
import com.shizq.bika.feature.reader.impl.util.rememberTopEndSystemAwarePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(viewModel: ReaderViewModel = hiltViewModel(), onBackClick: () -> Unit) {
    val uiState by viewModel.stateFlow.collectAsStateWithLifecycle()

    val imageList = viewModel.imageListFlow.collectAsLazyPagingItems()
    val chapterList = viewModel.chapterListFlow.collectAsLazyPagingItems()

    ReaderContent(
        state = uiState,
        imageList = imageList,
        chapterList = chapterList,
        onBackClick = onBackClick,
        dispatch = viewModel::dispatch,
        onFlushProgress = { viewModel.saveProgress(it) },
    )
}

@Composable
fun AutoScrollOverlay(
    isScrolling: Boolean,
    speed: Int,
    onPlayPauseToggle: () -> Unit,
    onSpeedUp: () -> Unit,
    onSpeedDown: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.75f),
        contentColor = Color.White,
        modifier = modifier
            .padding(16.dp)
            .width(56.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            IconButton(onClick = onPlayPauseToggle) {
                Icon(
                    imageVector = if (isScrolling) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isScrolling) "暂停" else "开始",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onSpeedDown,
                enabled = speed > 1
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "减速",
                    tint = if (speed > 1) Color.White else Color.Gray
                )
            }

            Text(
                text = "v$speed",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(
                onClick = onSpeedUp,
                enabled = speed < 10
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "加速",
                    tint = if (speed < 10) Color.White else Color.Gray
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "退出自动滚动",
                    tint = Color.Red
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(
    imageList: LazyPagingItems<ChapterPage>,
    chapterList: LazyPagingItems<Chapter>,
    state: ReaderUiState,
    onBackClick: () -> Unit = {},
    dispatch: (ReaderAction) -> Unit = {},
    onFlushProgress: (Int) -> Boolean = { false },
) {
    when (state) {
        is ReaderUiState.Initializing -> FullScreenLoading()
        is ReaderUiState.Ready -> {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val config = state.config
            val chapterState = state.chapter
            val overlayState = state.uiControl

            val readerContext = rememberReaderContext(
                readingMode = config.readingMode,
                chapterPages = imageList,
                config = config,
                initialPageIndex = chapterState.initialPage,
                chapterOrder = chapterState.order,
            )
            val controller = readerContext.controller

            val progressManager = rememberReadingProgressManager(
                controller = controller,
                imageList = imageList,
                initialPage = chapterState.initialPage,
                onPersist = onFlushProgress
            )

            // 监听进度恢复状态（用于调试和日志）
            val progressState by progressManager.state.collectAsStateWithLifecycle()
            LaunchedEffect(progressState) {
                when (val state = progressState) {
                    is ProgressState.Restoring -> {
                        BikaLog.d("ReaderScreen", "正在恢复进度到第 ${state.targetPage} 页")
                    }
                    is ProgressState.Restored -> {
                        BikaLog.d("ReaderScreen", "进度已恢复到第 ${state.actualPage} 页")
                    }
                    is ProgressState.RestoreFailed -> {
                        BikaLog.w("ReaderScreen", "进度恢复失败: ${state.reason}")
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
            val prevChapter = navigation.prev
            val nextChapter = navigation.next
            val hasNextChapter = nextChapter != null

            var isAutoScrolling by remember { mutableStateOf(false) }
            var isUserInteracting by remember { mutableStateOf(false) }

            LaunchedEffect(config.autoScrollEnabled) {
                isAutoScrolling = config.autoScrollEnabled
            }

            // 自动滚动依赖像素级连续滚动，只有条漫模式支持
            val supportsAutoScroll = controller.supportsContinuousScroll

            val isDragged by controller.interactionSource.collectIsDraggedAsState()
            LaunchedEffect(isDragged) {
                if (isDragged) {
                    isUserInteracting = true
                } else {
                    delay(1500)
                    isUserInteracting = false
                }
            }

            LaunchedEffect(
                isAutoScrolling,
                isUserInteracting,
                config.autoScrollSpeed,
                supportsAutoScroll,
                hasNextChapter,
            ) {
                if (isAutoScrolling && !isUserInteracting && supportsAutoScroll) {
                    while (true) {
                        if (controller.canScrollForward) {
                            controller.scrollBy(config.autoScrollSpeed.toFloat())
                            delay(16)
                        } else {
                            if (!hasNextChapter) {
                                isAutoScrolling = false
                                dispatch(SetAutoScrollEnabled(false))
                                Toast.makeText(context, "已到达全书底部", Toast.LENGTH_SHORT).show()
                                break
                            } else {
                                delay(200)
                            }
                        }
                    }
                }
            }

            // 当前页（提升到此层级，用于自动衔接检测）
            val currentPage by controller.visibleItemIndex.collectAsState(0)

            SystemUiController(showSystemUI = overlayState.showSystemBars)
            KeepScreenOnEffect()
            OrientationEffect(config.screenOrientation)
            ReaderBottomSheet(overlayState.readerSheet, config, dispatch)

            val onBack = {
                // 进度管理器会在 onDispose 时自动保存，这里只需退出
                onBackClick()
            }
            // TODO: 暂时移除
//            BackHandler(onBack = onBack)

            LaunchedEffect(overlayState.seekState) {
                if (overlayState.seekState is SeekState.Seeking) {
                    controller.scrollToPage(overlayState.seekState.targetPage.toInt())
                    dispatch(ReaderAction.SeekConsumed)
                }
            }

            // 自动衔接：到达当前章节最后一页时，自动跳转到下一章。如果是最后一章，提示后面没有内容了。
            // 用 chapterState.totalPages 作为 key 而非 snapshotFlow { chapterState.totalPages }：
            // chapterState 是普通局部值不是 Compose State，snapshotFlow 无依赖可订阅，
            // totalPages 初始为 0 时 first() 会永久挂起。改为 key 后，totalPages 从 0 变为非零值
            // 会触发 recomposition 重启这个 effect，天然实现“等待章节加载完成后再监听”。
            LaunchedEffect(chapterState.order, chapterState.totalPages, hasNextChapter) {
                val total = chapterState.totalPages
                if (total <= 0) return@LaunchedEffect
                // 监听页面到达末尾（停留 800ms 确认用户确实看到最后一页）
                controller.visibleItemIndex
                    .debounce(800)
                    .collect { page ->
                        if (page >= total - 1) {
                            delay(300)
                            if (nextChapter != null) {
                                // 自动跳转下一章，从头开始阅读，不恢复该章历史进度
                                dispatch(JumpToChapter(nextChapter, startFromBeginning = true, currentPage = page))
                            } else {
                                Toast.makeText(context, "后面没有内容了", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }

            // 智适应翻页速率预载计算
            var lastPageChangeTime by remember { mutableLongStateOf(0L) }
            val pageTimes = remember { mutableStateListOf<Long>() }
            var smartPreloadCount by remember(config.preloadCount) { mutableIntStateOf(config.preloadCount) }

            var draggedPage by remember { mutableStateOf<Int?>(null) }

            LaunchedEffect(currentPage) {
                val now = System.currentTimeMillis()
                if (lastPageChangeTime > 0) {
                    val diff = now - lastPageChangeTime
                    if (diff in 100..10000) { // 剔除异常值或跳章等耗时
                        pageTimes.add(diff)
                        if (pageTimes.size > 3) {
                            pageTimes.removeAt(0)
                        }
                        if (pageTimes.size == 3) {
                            val avg = pageTimes.average()
                            smartPreloadCount = when {
                                config.preloadCount == 0 -> 0
                                avg < 1500 -> 6 // 快速扫读
                                avg > 3200 -> 2 // 慢速精读
                                else -> config.preloadCount
                            }
                        }
                    }
                }
                lastPageChangeTime = now
            }

            val preloadModelProvider = remember(context) { ChapterPagePreloadProvider(context) }
            PagingPreload(
                pagingItems = imageList,
                scrollStateProvider = readerContext.scrollStateProvider,
                modelProvider = preloadModelProvider,
                preloadCount = smartPreloadCount
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
                        TopBar(title = { Text(title) }, onBackClick = onBack)
                    },
                    bottomBar = {
                        LiveReaderBottomBar(
                            currentPage = currentPage,
                            totalPages = chapterState.totalPages,
                            readingMode = config.readingMode,
                            onSeekToPage = {
                                scope.launch { controller.scrollToPage(it) }
                            },
                            onToggleChapterList = { dispatch(ShowSheet(ReaderSheet.ChapterList)) },
                            onOpenSettings = { dispatch(ShowSheet(ReaderSheet.Settings)) },
                            onOpenReadingMode = { dispatch(ShowSheet(ReaderSheet.ReadingMode)) },
                            onOpenOrientation = { dispatch(ShowSheet(ReaderSheet.Orientation)) },
                            hasPrevChapter = prevChapter != null,
                            hasNextChapter = hasNextChapter,
                            onPrevChapter = {
                                prevChapter?.let { dispatch(JumpToChapter(it, currentPage = currentPage)) }
                            },
                            onNextChapter = {
                                if (nextChapter != null) {
                                    dispatch(JumpToChapter(nextChapter, currentPage = currentPage))
                                } else {
                                    Toast.makeText(context, "后面没有内容了", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onSeeking = { draggedPage = it },
                            onSeekingFinished = { draggedPage = null }
                        )
                    },
                    floatingMessage = {
                        if (chapterState.totalPages > 0) {
                            LivePageIndicatorBadge(
                                controller = controller,
                                total = chapterState.totalPages
                            )
                        }
                    },
                    sideSheet = {
                        val isChapterListVisible =
                            overlayState.readerSheet is ReaderSheet.ChapterList
                        AnimatedVisibility(
                            visible = isChapterListVisible,
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
                                        Icon(Icons.Rounded.Close, contentDescription = "关闭目录")
                                    }
                                }
                            ) {
                                ChapterList(
                                    chapters = chapterList,
                                    currentChapterOrder = chapterState.order,
                                    onChapterClick = { newChapter ->
                                        dispatch(JumpToChapter(newChapter, currentPage = currentPage))
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    },
                    content = {
                        val gestureState = rememberGestureState(
                            layout = config.tapZoneLayout,
                            isRtl = config.readingMode.isRtl,
                        )
                        ReaderLayoutHost(
                            readerContext = readerContext,
                            gestureState = gestureState,
                            pageItems = imageList,
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
                if (config.autoScrollEnabled && supportsAutoScroll) {
                    AutoScrollOverlay(
                        isScrolling = isAutoScrolling,
                        speed = config.autoScrollSpeed,
                        onPlayPauseToggle = { isAutoScrolling = !isAutoScrolling },
                        onSpeedUp = {
                            if (config.autoScrollSpeed < 10) {
                                dispatch(SetAutoScrollSpeed(config.autoScrollSpeed + 1))
                            }
                        },
                        onSpeedDown = {
                            if (config.autoScrollSpeed > 1) {
                                dispatch(SetAutoScrollSpeed(config.autoScrollSpeed - 1))
                            }
                        },
                        onClose = {
                            dispatch(SetAutoScrollEnabled(false))
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

                if (draggedPage != null) {
                    val previewPage = draggedPage!!
                    val pageUrl = if (previewPage in 0 until imageList.itemCount) imageList.peek(previewPage)?.url else null
                    ScrubPreviewCard(
                        pageUrl = pageUrl,
                        currentPage = previewPage,
                        totalPages = chapterState.totalPages,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            }
        }
    }
}

@Composable
fun PageIndicatorBadge(current: Int, total: Int) {
    Text(
        text = "$current / $total",
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
private fun SystemUiController(showSystemUI: Boolean) {
    val window = LocalWindow.current

    DisposableEffect(window, showSystemUI) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (showSystemUI) {
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
private fun LivePageIndicatorBadge(controller: ReaderController, total: Int) {
    val current by controller.visibleItemIndex.collectAsState(0)
    PageIndicatorBadge(current = current + 1, total = total)
}

@Composable
private fun ScrubPreviewCard(
    pageUrl: String?,
    currentPage: Int,
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
                    contentDescription = "Preview Page ${currentPage + 1}",
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
                    text = "${currentPage + 1} / $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LiveReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    readingMode: ReadingMode,
    onSeekToPage: (Int) -> Unit,
    onToggleChapterList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReadingMode: () -> Unit,
    onOpenOrientation: () -> Unit,
    hasPrevChapter: Boolean = false,
    hasNextChapter: Boolean = false,
    onPrevChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onSeeking: ((Int) -> Unit)? = null,
    onSeekingFinished: (() -> Unit)? = null,
) {
    ReaderBottomBar(
        currentPage = currentPage,
        totalPages = totalPages,
        readingMode = readingMode,
        onSeekToPage = onSeekToPage,
        onToggleChapterList = onToggleChapterList,
        onOpenSettings = onOpenSettings,
        onOpenReadingMode = onOpenReadingMode,
        onOpenOrientation = onOpenOrientation,
        hasPrevChapter = hasPrevChapter,
        hasNextChapter = hasNextChapter,
        onPrevChapter = onPrevChapter,
        onNextChapter = onNextChapter,
        onSeeking = onSeeking,
        onSeekingFinished = onSeekingFinished,
    )
}