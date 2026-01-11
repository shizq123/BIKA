package com.shizq.bika.ui.reader

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.core.context.findActivity
import com.shizq.bika.core.model.ScreenOrientation
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
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(viewModel: ReaderViewModel = hiltViewModel(), onBackClick: () -> Unit) {
    val uiState by viewModel.stateFlow.collectAsStateWithLifecycle()

    val imageList = viewModel.imageListFlow.collectAsLazyPagingItems()
    val chapterList = viewModel.chapterListFlow.collectAsLazyPagingItems()

    KeepScreenOnEffect()

    ReaderContent(
        state = uiState,
        imageList = imageList,
        chapterList = chapterList,
        onBackClick = onBackClick,
        dispatch = viewModel::dispatch,
    )
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
        ReaderUiState.Initializing -> {}
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
                initialPageIndex = chapterState.initialPage
            )
            val controller = readerContext.controller

            SystemUiController(showSystemUI = overlayState.showSystemBars)
            OrientationEffect(config.screenOrientation)
            ReaderBottomSheet(overlayState.readerSheet, config, dispatch)

            LaunchedEffect(overlayState.seekState) {
                if (overlayState.seekState is SeekState.Seeking) {
                    controller.scrollToPage(overlayState.seekState.targetPage.toInt())
                    dispatch(ReaderAction.SeekConsumed)
                }
            }

            val visibleItemIndex by controller.visibleItemIndex.collectAsState(0)

            DisposableEffect(visibleItemIndex) {
                dispatch(SyncReadingProgress(visibleItemIndex))
                onDispose {
                    dispatch(SyncReadingProgress(visibleItemIndex))
                }
            }

            val preloadModelProvider = remember(context) { ChapterPagePreloadProvider(context) }
            PagingPreload(
                pagingItems = imageList,
                scrollStateProvider = readerContext.scrollStateProvider,
                modelProvider = preloadModelProvider,
                preloadCount = config.preloadCount
            )

            ReaderScaffold(
                showMenu = overlayState.showSystemBars,
                topBar = {
                    val title = chapterState.meta?.title ?: "Chapter ${chapterState.order}"
                    TopBar(title = { Text(title) }, onBackClick = onBackClick)
                },
                bottomBar = {
                    ReaderBottomBar(
                        currentPage = visibleItemIndex,
                        totalPages = chapterState.totalPages,
                        readingMode = config.readingMode,
                        onSeekToPage = {
                            scope.launch { controller.scrollToPage(it) }
                        },
                        onToggleChapterList = { dispatch(ShowSheet(ReaderSheet.ChapterList)) },
                        onOpenSettings = { dispatch(ShowSheet(ReaderSheet.Settings)) },
                        onOpenReadingMode = { dispatch(ShowSheet(ReaderSheet.ReadingMode)) },
                        onOpenOrientation = { dispatch(ShowSheet(ReaderSheet.Orientation)) }
                    )
                },
                floatingMessage = {
                    if (chapterState.totalPages > 0) {
                        PageIndicatorBadge(
                            current = visibleItemIndex + 1,
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
    val view = LocalView.current
    val window = (view.context as? Activity)?.window

    DisposableEffect(Unit) {
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            onDispose {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            onDispose {}
        }
    }
}

@Composable
private fun SystemUiController(showSystemUI: Boolean) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window ?: return

    LaunchedEffect(window, showSystemUI) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (showSystemUI) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}