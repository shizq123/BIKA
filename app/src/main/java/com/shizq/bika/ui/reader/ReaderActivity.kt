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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.shizq.bika.ui.reader.layout.ReaderConfig
import com.shizq.bika.ui.reader.layout.ReaderLayout
import com.shizq.bika.ui.reader.layout.rememberReaderContext
import com.shizq.bika.ui.reader.settings.SettingsScreen
import com.shizq.bika.ui.reader.util.ImagePreloader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
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
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val chapterMetadata = uiState.chapterMeta
        val chapterOrder = uiState.currentChapterOrder
        val readerPreferences = uiState.readerConfig

        val imageList = viewModel.imageListFlow.collectAsLazyPagingItems()
        val chapterList = viewModel.chapterListFlow.collectAsLazyPagingItems()

        OrientationEffect(readerPreferences.screenOrientation)

        ReaderContent(
            imageList = imageList,
            chapterList = chapterList,
            title = chapterMetadata?.title ?: "",
            pageCount = chapterMetadata?.totalImages ?: 0,
            onBackClick = onBackClick,
            highlightedChapter = chapterOrder,
            onChapterChange = viewModel::onChapterChange,
            readerPreferences = readerPreferences,
            initialPageIndex = 0,
            onProgressUpdate = viewModel::saveProgress,
            getHistoryPage = viewModel::getChapterHistoryPage
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ReaderContent(
        imageList: LazyPagingItems<ChapterPage>,
        chapterList: LazyPagingItems<Chapter>,
        title: String,
        pageCount: Int,
        initialPageIndex: Int,
        onProgressUpdate: (Int) -> Unit = {},
        onBackClick: () -> Unit = {},
        highlightedChapter: Int,
        getHistoryPage: suspend (Int) -> Int = { 0 },
        onChapterChange: (Chapter) -> Unit = {},
        readerPreferences: ReaderConfig
    ) {
        val readerContext = rememberReaderContext(
            readerPreferences.readingMode,
            imageList,
            readerPreferences,
            initialPageIndex
        )
        val controller = readerContext.controller
        val scope = rememberCoroutineScope()

        var currentUiPage by remember { mutableIntStateOf(initialPageIndex) }
        var isDraggingSlider by remember { mutableStateOf(false) }

        var showMenu by rememberSaveable { mutableStateOf(false) }
        var showChapterList by remember { mutableStateOf(false) }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        val settingsSheetState = rememberModalBottomSheetState(true)

        SystemUiController(showSystemUI = showMenu)

        LaunchedEffect(highlightedChapter) {
            val targetPage = getHistoryPage(highlightedChapter)

            if (targetPage > 0) {
                controller.scrollToPage(targetPage)
            } else {
                controller.scrollToPage(0)
            }
        }

        LaunchedEffect(controller) {
            controller.visibleItemIndex
                .distinctUntilChanged()
                .collect { page ->
                    if (!isDraggingSlider) {
                        currentUiPage = page
                    }
                    onProgressUpdate(page)
                }
        }

        ImagePreloader(
            imageList = imageList,
            firstVisibleIndex = currentUiPage,
            preloadCount = readerPreferences.preloadCount
        )

        ReaderScaffold(
            showMenu = showMenu,
            topBar = {
                TopBar(title = { Text(title) }, onBackClick = onBackClick)
            },
            bottomBar = {
                BottomBar(
                    progressIndicator = {
                        if (pageCount > 0) {
                            Text(text = "${currentUiPage + 1} / $pageCount")
                        }
                    },
                    progressSlider = {
                        Slider(
                            value = currentUiPage.toFloat(),
                            onValueChange = { newValue ->
                                isDraggingSlider = true
                                currentUiPage = newValue.toInt()
                            },
                            onValueChangeFinished = {
                                scope.launch {
                                    controller.scrollToPage(currentUiPage)
                                    isDraggingSlider = false
                                }
                            },
                            valueRange = 0f..(pageCount.coerceAtLeast(1) - 1).toFloat(),
                        )
                    },
                    startActions = {
                        IconButton(onClick = { showChapterList = true }) {
                            Icon(Icons.Default.Menu, "目录")
                        }
                    },
                    middleActions = {

                    },
                    endActions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, "设置")
                        }
                    }
                )
            },
            floatingMessage = {
                Text(
                    text = "${currentUiPage + 1} / $pageCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            },
            sideSheet = {
                AnimatedVisibility(
                    showChapterList,
                    enter = slideInHorizontally(tween()),
                    exit = slideOutHorizontally(tween()),
                ) {
                    SideSheetLayout(
                        title = { Text("目录") },
                        onDismissRequest = { showChapterList = false },
                        closeButton = {
                            IconButton(onClick = { showChapterList = false }) {
                                Icon(Icons.Default.Close, contentDescription = "关闭目录")
                            }
                        }
                    ) {
                        ChapterList(
                            chapters = chapterList,
                            onChapterClick = { newChapterId ->
                                onChapterChange(newChapterId)
                                showChapterList = false // 点击章节后关闭面板
                                showMenu = false
                            },
                            modifier = Modifier.padding(top = 8.dp),
                            currentChapterId = highlightedChapter
                        )
                    }
                }
            },
            content = {
                val gestureState = rememberGestureState(readerPreferences.tapZoneLayout)
                ReaderLayout(
                    readerContext = readerContext,
                    gestureState = gestureState,
                    chapterPages = imageList,
                    toggleMenuVisibility = { showMenu = !showMenu }
                )
            }
        )

        if (showSettings) {
            SettingsScreen(settingsSheetState = settingsSheetState) {
                scope.launch { settingsSheetState.hide() }.invokeOnCompletion {
                    if (!settingsSheetState.isVisible) {
                        showSettings = false
                    }
                }
            }

            // 底部留白，防止在全面屏手机上贴底太近
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    @Composable
    fun OrientationEffect(currentOrientation: ScreenOrientation) {
        val context = LocalContext.current
        LaunchedEffect(currentOrientation) {
            val activity = context.findActivity()
            activity?.requestedOrientation = when (currentOrientation) {
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