@file:OptIn(FlowPreview::class)

package com.shizq.bika.ui.reader

import kotlinx.coroutines.FlowPreview
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.shizq.bika.ui.reader.layout.LocalReaderConfig
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.core.context.findActivity
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.core.model.ScreenOrientation
import com.shizq.bika.core.ui.FullScreenLoading
import com.shizq.bika.core.ui.composition.LocalWindow
import com.shizq.bika.paging.Chapter
import com.shizq.bika.paging.ChapterPage
import com.shizq.bika.ui.reader.bar.ReaderBottomBar
import com.shizq.bika.ui.reader.bar.TopBar
import com.shizq.bika.ui.reader.components.ChapterList
import com.shizq.bika.ui.reader.components.ReadingModeSelectBottomSheet
import com.shizq.bika.ui.reader.components.ReadingSettingsBottomSheet
import com.shizq.bika.ui.reader.components.ScreenOrientationSelectBottomSheet
import com.shizq.bika.ui.reader.gesture.rememberGestureState
import com.shizq.bika.ui.reader.layout.ReaderConfig
import com.shizq.bika.ui.reader.layout.ReaderController
import com.shizq.bika.ui.reader.layout.ReaderLayout
import com.shizq.bika.ui.reader.layout.SideSheetLayout
import com.shizq.bika.ui.reader.layout.rememberReaderContext
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.state.ReaderAction.HideSheet
import com.shizq.bika.ui.reader.state.ReaderAction.JumpToChapter
import com.shizq.bika.ui.reader.state.ReaderAction.SetOrientation
import com.shizq.bika.ui.reader.state.ReaderAction.SetReadingMode
import com.shizq.bika.ui.reader.state.ReaderAction.ShowSheet
import com.shizq.bika.ui.reader.state.ReaderAction.SyncReadingProgress
import com.shizq.bika.ui.reader.state.ReaderAction.ToggleBarsVisibility
import com.shizq.bika.ui.reader.state.ReaderSheet
import com.shizq.bika.ui.reader.state.ReaderUiState
import com.shizq.bika.ui.reader.state.SeekState
import com.shizq.bika.ui.reader.util.preload.ChapterPagePreloadProvider
import com.shizq.bika.ui.reader.util.preload.PagingPreload
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import coil3.request.crossfade

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
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.75f),
        contentColor = Color.White,
        modifier = modifier
            .padding(16.dp)
            .width(56.dp)
    ) {
        androidx.compose.foundation.layout.Column(
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

            androidx.compose.material3.HorizontalDivider(
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

            // 首次进入章节时，若 initialPage > 0 且数据加载完成（itemCount > 0），则自动跳转至上次进度位置。
            var hasRestoredProgress by remember(chapterState.order) { mutableStateOf(false) }
            LaunchedEffect(chapterState.initialPage, imageList.itemCount, hasRestoredProgress) {
                if (!hasRestoredProgress && chapterState.initialPage > 0 && imageList.itemCount > 0) {
                    controller.scrollToPage(chapterState.initialPage)
                    hasRestoredProgress = true
                } else if (chapterState.initialPage == 0) {
                    hasRestoredProgress = true
                }
            }

            // 根据当前章节 order 从列表中找出相邻章节
            val currentChapterIndex = remember(chapterState.order, chapterList.itemCount) {
                (0 until chapterList.itemCount).firstOrNull {
                    chapterList.peek(it)?.order == chapterState.order
                }
            }
            val prevChapter: Chapter? = remember(currentChapterIndex, chapterList.itemCount, chapterState.order) {
                currentChapterIndex?.let { idx ->
                    if (idx > 0) chapterList.peek(idx - 1) else null
                } ?: if (chapterList.itemCount == 0 && chapterState.order > 1) {
                    // 章节列表还未加载完成时，用 order 推断前一章（兼容首次进入）
                    Chapter(id = "", order = chapterState.order - 1, title = "", updatedAt = "")
                } else null
            }
            val nextChapter: Chapter? = remember(currentChapterIndex, chapterList.itemCount, chapterState.order) {
                currentChapterIndex?.let { idx ->
                    if (idx < chapterList.itemCount - 1) chapterList.peek(idx + 1) else null
                } ?: if (chapterList.itemCount == 0) {
                    // 章节列表还未加载完成时，用 order 推断后一章（兼容首次进入）
                    Chapter(id = "", order = chapterState.order + 1, title = "", updatedAt = "")
                } else null
            }

            var isAutoScrolling by remember { mutableStateOf(false) }
            var isUserInteracting by remember { mutableStateOf(false) }

            LaunchedEffect(config.autoScrollEnabled) {
                isAutoScrolling = config.autoScrollEnabled
            }

            val listState = readerContext.lazyListState
            if (listState != null) {
                val isDragged by listState.interactionSource.collectIsDraggedAsState()
                LaunchedEffect(isDragged) {
                    if (isDragged) {
                        isUserInteracting = true
                    } else {
                        delay(1500)
                        isUserInteracting = false
                    }
                }
            }

            LaunchedEffect(isAutoScrolling, isUserInteracting, config.autoScrollSpeed, listState, nextChapter) {
                if (isAutoScrolling && !isUserInteracting && listState != null) {
                    while (true) {
                        val canScroll = listState.canScrollForward
                        if (canScroll) {
                            controller.scrollBy(config.autoScrollSpeed.toFloat())
                            delay(16)
                        } else {
                            if (nextChapter == null) {
                                isAutoScrolling = false
                                dispatch(ReaderAction.SetAutoScrollEnabled(false))
                                android.widget.Toast.makeText(context, "已到达全书底部", android.widget.Toast.LENGTH_SHORT).show()
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
                dispatch(SyncReadingProgress(currentPage))
                onBackClick()
            }
            BackHandler(onBack = onBack)

            LaunchedEffect(overlayState.seekState) {
                if (overlayState.seekState is SeekState.Seeking) {
                    controller.scrollToPage(overlayState.seekState.targetPage.toInt())
                    dispatch(ReaderAction.SeekConsumed)
                }
            }

            LaunchedEffect(controller) {
                controller.visibleItemIndex
                    .debounce(1000)
                    .collect { index ->
                        dispatch(SyncReadingProgress(index))
                    }
            }

            // 自动衔接：到达当前章节最后一页时，自动跳转到下一章。如果是最后一章，提示后面没有内容了。
            LaunchedEffect(chapterState.order, nextChapter) {
                // 等待章节加载完成（totalPages > 0）
                val total = snapshotFlow { chapterState.totalPages }
                    .filter { it > 0 }
                    .first()
                // 监听页面到达末尾（停留 800ms 确认用户确实看到最后一页）
                controller.visibleItemIndex
                    .debounce(800)
                    .collect { page ->
                        if (page >= total - 1) {
                            delay(300)
                            if (nextChapter != null) {
                                // 自动跳转下一章，从头开始阅读，不恢复该章历史进度
                                dispatch(SyncReadingProgress(currentPage))
                                dispatch(JumpToChapter(nextChapter, startFromBeginning = true))
                            } else {
                                android.widget.Toast.makeText(context, "后面没有内容了", android.widget.Toast.LENGTH_SHORT).show()
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
                            controller = controller,
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
                            onPrevChapter = prevChapter?.let { ch -> {
                                dispatch(SyncReadingProgress(currentPage))
                                dispatch(JumpToChapter(ch))
                            } },
                            onNextChapter = nextChapter?.let { ch -> {
                                dispatch(SyncReadingProgress(currentPage))
                                dispatch(JumpToChapter(ch))
                            } }
                                ?: { android.widget.Toast.makeText(context, "后面没有内容了", android.widget.Toast.LENGTH_SHORT).show() },
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
                                    currentChapterId = chapterState.order,
                                    onChapterClick = { newChapter ->
                                        dispatch(SyncReadingProgress(currentPage))
                                        dispatch(JumpToChapter(newChapter))
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    },
                    content = {
                        val gestureState = rememberGestureState(config.tapZoneLayout)
                        ReaderLayout(
                            readerContext = readerContext,
                            gestureState = gestureState,
                            chapterPages = imageList,
                            toggleMenuVisibility = { dispatch(ToggleBarsVisibility) },
                            onHideMenu = {
                                if (overlayState.showSystemBars) {
                                    dispatch(ToggleBarsVisibility)
                                }
                            }
                        )
                    }
                )

                if (config.autoScrollEnabled) {
                    AutoScrollOverlay(
                        isScrolling = isAutoScrolling,
                        speed = config.autoScrollSpeed,
                        onPlayPauseToggle = { isAutoScrolling = !isAutoScrolling },
                        onSpeedUp = {
                            if (config.autoScrollSpeed < 10) {
                                dispatch(ReaderAction.SetAutoScrollSpeed(config.autoScrollSpeed + 1))
                            }
                        },
                        onSpeedDown = {
                            if (config.autoScrollSpeed > 1) {
                                dispatch(ReaderAction.SetAutoScrollSpeed(config.autoScrollSpeed - 1))
                            }
                        },
                        onClose = {
                            dispatch(ReaderAction.SetAutoScrollEnabled(false))
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }

                if (config.statusBarCapsuleEnabled && !overlayState.showSystemBars) {
                    StatusBarCapsule(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 12.dp)
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
private fun StatusBarCapsule(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var clockTime by remember { mutableStateOf("") }
    var batteryPct by remember { mutableStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    val batteryManager = remember(context) {
        context.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
    }

    fun updateStatus() {
        val calendar = java.util.Calendar.getInstance()
        val hour = String.format("%02d", calendar.get(java.util.Calendar.HOUR_OF_DAY))
        val minute = String.format("%02d", calendar.get(java.util.Calendar.MINUTE))
        clockTime = "$hour:$minute"

        batteryPct = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        isCharging = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            batteryManager.isCharging
        } else {
            false
        }
    }

    LaunchedEffect(Unit) {
        updateStatus()
        while (true) {
            delay(15000)
            updateStatus()
        }
    }

    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            Color.White.copy(alpha = 0.15f)
        ),
        modifier = modifier
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = clockTime,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )

            val batteryColor = when {
                isCharging -> Color(0xFF4CAF50)
                batteryPct <= 20 -> Color(0xFFF44336)
                else -> Color.White.copy(alpha = 0.8f)
            }

            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isCharging) {
                    Text(
                        text = "⚡",
                        style = MaterialTheme.typography.labelSmall,
                        color = batteryColor
                    )
                }
                Text(
                    text = "$batteryPct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = batteryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ScrubPreviewCard(
    pageUrl: String?,
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(
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
                coil3.compose.AsyncImage(
                    model = coil3.request.ImageRequest.Builder(LocalContext.current)
                        .data(pageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Preview Page ${currentPage + 1}",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
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
    controller: ReaderController,
    currentPage: Int,
    totalPages: Int,
    readingMode: ReadingMode,
    onSeekToPage: (Int) -> Unit,
    onToggleChapterList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReadingMode: () -> Unit,
    onOpenOrientation: () -> Unit,
    onPrevChapter: (() -> Unit)? = null,
    onNextChapter: (() -> Unit)? = null,
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
        onPrevChapter = onPrevChapter,
        onNextChapter = onNextChapter,
        onSeeking = onSeeking,
        onSeekingFinished = onSeekingFinished,
    )
}