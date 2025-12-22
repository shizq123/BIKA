package com.shizq.bika.ui.reader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.paging.compose.itemKey
import com.shizq.bika.core.context.findActivity
import com.shizq.bika.core.model.ScreenOrientation
import com.shizq.bika.paging.Chapter
import com.shizq.bika.paging.ChapterPage
import com.shizq.bika.ui.reader.bar.BottomBar
import com.shizq.bika.ui.reader.bar.TopBar
import com.shizq.bika.ui.reader.gesture.rememberGestureState
import com.shizq.bika.ui.reader.layout.ReaderLayout
import com.shizq.bika.ui.reader.layout.SideSheetLayout
import com.shizq.bika.ui.reader.layout.rememberReaderContext
import com.shizq.bika.ui.reader.settings.SettingsScreen
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.state.ReaderUiState
import com.shizq.bika.ui.reader.state.SeekState
import com.shizq.bika.ui.reader.util.preload.ChapterPagePreloadProvider
import com.shizq.bika.ui.reader.util.preload.PagingPreload
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReaderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            ReaderScreen(onBackClick = { finish() })
        }
    }

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
                val overlayState = state.overlay

                var currentUiPage by remember { mutableIntStateOf(0) }

                val readerContext = rememberReaderContext(
                    readingMode = config.readingMode,
                    chapterPages = imageList,
                    config = config,
                    initialPageIndex = 0
                )
                val controller = readerContext.controller

                SystemUiController(showSystemUI = overlayState.isMenuVisible)
                OrientationEffect(config.screenOrientation)

                LaunchedEffect(overlayState.seekState) {
                    when (val seek = overlayState.seekState) {
                        is SeekState.Seeking -> {
                            controller.scrollToPage(seek.progress.toInt())
                        }

                        SeekState.Idle -> {}
                    }
                }

                LaunchedEffect(controller) {
                    controller.visibleItemIndex
                        .debounce(100)
                        .collect { page ->
                            currentUiPage = page
                            dispatch(ReaderAction.SaveProgress(page))
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
                    showMenu = overlayState.isMenuVisible,
                    topBar = {
                        val title = chapterState.meta?.title ?: "Chapter ${chapterState.order}"
                        TopBar(title = { Text(title) }, onBackClick = onBackClick)
                    },
                    bottomBar = {
                        BottomBar(
                            progressIndicator = {
                                if (chapterState.totalPages > 0) {
                                    Text(text = "${currentUiPage + 1} / ${chapterState.totalPages}")
                                }
                            },
                            progressSlider = {
                                Slider(
                                    value = currentUiPage.toFloat(),
                                    onValueChange = { newValue ->
                                        currentUiPage = newValue.toInt()
                                    },
                                    onValueChangeFinished = {
                                        scope.launch {
                                            controller.scrollToPage(currentUiPage)
                                        }
                                    },
                                    valueRange = 0f..(chapterState.totalPages.coerceAtLeast(1) - 1).toFloat(),
                                )
                            },
                            startActions = {
                                IconButton(onClick = { dispatch(ReaderAction.ToggleChapterList) }) {
                                    Icon(Icons.Default.Menu, "目录")
                                }
                            },
                            middleActions = {
                            },
                            endActions = {
                                IconButton(onClick = { dispatch(ReaderAction.ToggleSettings) }) {
                                    Icon(Icons.Default.Settings, "设置")
                                }
                            }
                        )
                    },
                    floatingMessage = {
                        if (chapterState.totalPages > 0) {
                            Text(
                                text = "${currentUiPage + 1} / ${chapterState.totalPages}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier
                                    .background(
                                        Color.Black.copy(alpha = 0.6f),
                                        MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    },
                    sideSheet = {
                        AnimatedVisibility(
                            visible = overlayState.isChapterListVisible,
                            enter = slideInHorizontally(
                                animationSpec = tween(),
                                initialOffsetX = { fullWidth -> -fullWidth }
                            ),
                            exit = slideOutHorizontally(
                                animationSpec = tween(),
                                targetOffsetX = { fullWidth -> -fullWidth }
                            ),
                        ) {
                            SideSheetLayout(
                                title = { Text("目录") },
                                onDismissRequest = { dispatch(ReaderAction.ToggleChapterList) },
                                closeButton = {
                                    IconButton(onClick = { dispatch(ReaderAction.ToggleChapterList) }) {
                                        Icon(Icons.Default.Close, contentDescription = "关闭目录")
                                    }
                                }
                            ) {
                                ChapterList(
                                    chapters = chapterList,
                                    currentChapterId = chapterState.order,
                                    onChapterClick = { newChapter ->
                                        dispatch(ReaderAction.ChangeChapter(newChapter))
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
                            toggleMenuVisibility = { dispatch(ReaderAction.ToggleMenu) }
                        )
                    }
                )

                if (overlayState.isSettingsVisible) {
                    val settingsSheetState =
                        rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    SettingsScreen(
                        settingsSheetState = settingsSheetState,
                    ) {
                        scope.launch { settingsSheetState.hide() }.invokeOnCompletion {
                            if (!settingsSheetState.isVisible) {
                                dispatch(ReaderAction.ToggleSettings)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
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
    fun ChapterList(
        chapters: LazyPagingItems<Chapter>,
        currentChapterId: Int,
        onChapterClick: (Chapter) -> Unit,
        modifier: Modifier = Modifier
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            item {
                Text(
                    text = "章节列表",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider()
            }

            items(
                count = chapters.itemCount,
                key = chapters.itemKey { it.id }
            ) { index ->
                chapters[index]?.let { chapter ->
                    val isCurrent = chapter.order == currentChapterId
                    ChapterListItem(
                        chapter = chapter,
                        isCurrent = isCurrent,
                        onClick = { onChapterClick(chapter) },
                    )
                }
            }
        }
    }

    @Composable
    fun ChapterListItem(
        chapter: Chapter,
        isCurrent: Boolean,
        onClick: () -> Unit
    ) {
        val backgroundColor = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            Color.Transparent
        }

        val titleColor = if (isCurrent) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isCurrent) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Playing", tint = titleColor)
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

    companion object {
        internal const val EXTRA_ID = "com.shizq.bika.reader.EXTRA_BOOK_ID"
        internal const val EXTRA_ORDER = "com.shizq.bika.reader.EXTRA_ORDER"
        internal const val EXTRA_TOTAL_EPS = "com.shizq.bika.reader.EXTRA_TOTAL_EPS"
        internal const val EXTRA_TITLE = "com.shizq.bika.reader.EXTRA_TITLE"

        @Deprecated("Deprecated in Kotlin", ReplaceWith("start(context, bookId, order)"))
        fun start(context: Context, bookId: String, order: Int, totalEps: Int, title: String) {
            val intent = Intent(context, ReaderActivity::class.java)
            intent.putExtra(EXTRA_ID, bookId)
            intent.putExtra(EXTRA_ORDER, order)
            intent.putExtra(EXTRA_TOTAL_EPS, totalEps)
            intent.putExtra(EXTRA_TITLE, title)
            context.startActivity(intent)
        }

        fun start(context: Context, bookId: String, order: Int) {
            val intent = Intent(context, ReaderActivity::class.java)
            intent.putExtra(EXTRA_ID, bookId)
            intent.putExtra(EXTRA_ORDER, order)
            context.startActivity(intent)
        }
    }
}