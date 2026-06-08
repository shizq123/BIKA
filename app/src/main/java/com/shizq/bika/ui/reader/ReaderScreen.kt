package com.shizq.bika.ui.reader

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
                enabled = speed < 5
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "加速",
                    tint = if (speed < 5) Color.White else Color.Gray
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

            LaunchedEffect(isAutoScrolling, isUserInteracting, config.autoScrollSpeed, listState) {
                if (isAutoScrolling && !isUserInteracting && listState != null) {
                    while (true) {
                        val canScroll = listState.canScrollForward
                        if (canScroll) {
                            controller.scrollBy(config.autoScrollSpeed.toFloat())
                        } else {
                            isAutoScrolling = false
                            android.widget.Toast.makeText(context, "已到达本章底部", android.widget.Toast.LENGTH_SHORT).show()
                            break
                        }
                        delay(16)
                    }
                }
            }

            // 当前页（提升到此层级，用于自动衔接检测）
            val currentPage by controller.visibleItemIndex.collectAsState(0)

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

            SystemUiController(showSystemUI = overlayState.showSystemBars)
            KeepScreenOnEffect()
            OrientationEffect(config.screenOrientation)
            ReaderBottomSheet(overlayState.readerSheet, config, dispatch)

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
                                dispatch(JumpToChapter(nextChapter, startFromBeginning = true))
                            } else {
                                android.widget.Toast.makeText(context, "后面没有内容了", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }

            val preloadModelProvider = remember(context) { ChapterPagePreloadProvider(context) }
            PagingPreload(
                pagingItems = imageList,
                scrollStateProvider = readerContext.scrollStateProvider,
                modelProvider = preloadModelProvider,
                preloadCount = config.preloadCount
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
                            onPrevChapter = prevChapter?.let { ch -> { dispatch(JumpToChapter(ch)) } },
                            onNextChapter = nextChapter?.let { ch -> { dispatch(JumpToChapter(ch)) } }
                                ?: { android.widget.Toast.makeText(context, "后面没有内容了", android.widget.Toast.LENGTH_SHORT).show() },
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
                            if (config.autoScrollSpeed < 5) {
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
    )
}