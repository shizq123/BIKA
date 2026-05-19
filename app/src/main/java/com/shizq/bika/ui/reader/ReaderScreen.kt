package com.shizq.bika.ui.reader

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.core.ui.FullScreenLoading
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
import com.shizq.bika.ui.reader.layout.ReaderContext
import com.shizq.bika.ui.reader.layout.ReaderLayout
import com.shizq.bika.ui.reader.layout.SideSheetLayout
import com.shizq.bika.ui.reader.layout.rememberReaderContext
import com.shizq.bika.ui.reader.state.ChapterState
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.state.ReaderAction.HideSheet
import com.shizq.bika.ui.reader.state.ReaderAction.JumpToChapter
import com.shizq.bika.ui.reader.state.ReaderAction.SetOrientation
import com.shizq.bika.ui.reader.state.ReaderAction.SetReadingMode
import com.shizq.bika.ui.reader.state.ReaderAction.ShowSheet
import com.shizq.bika.ui.reader.state.ReaderAction.ToggleBarsVisibility
import com.shizq.bika.ui.reader.state.ReaderSheet
import com.shizq.bika.ui.reader.state.ReaderUiState
import com.shizq.bika.ui.reader.state.UiControlState
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
    )
}

@Composable
private fun ReaderContent(
    pageItems: LazyPagingItems<ChapterPage>,
    chapterItems: LazyPagingItems<Chapter>,
    state: ReaderUiState,
    onBackClick: () -> Unit = {},
    dispatch: (ReaderAction) -> Unit = {},
) {
    when (state) {
        is ReaderUiState.Initializing -> FullScreenLoading()
        is ReaderUiState.Ready -> {
            val scope = rememberCoroutineScope()
            val config = state.config
            val chapterState = state.chapter
            val uiControlState = state.uiControl

            val readerContext = rememberReaderContext(
                readingMode = config.readingMode,
                pageItems = pageItems,
                config = config,
                initialPageIndex = chapterState.initialPage,
            )
            val currentPage by readerContext.controller.visibleItemIndex.collectAsState(0)

            ReaderSideEffects(
                config = config,
                uiControl = uiControlState,
                controller = readerContext.controller,
                pageItems = pageItems,
                scrollStateProvider = readerContext.scrollStateProvider,
                preloadCount = config.preloadCount,
                dispatch = dispatch,
            )
            ReaderSheetHost(
                sheet = uiControlState.readerSheet,
                config = config,
                dispatch = dispatch,
            )

            ReaderScaffold(
                showMenu = uiControlState.showSystemBars,
                topBar = {
                    ReaderTopBar(
                        chapterState = chapterState,
                        onBackClick = onBackClick,
                    )
                },
                bottomBar = {
                    ReaderBottomBarHost(
                        currentPage = currentPage,
                        readingMode = config.readingMode,
                        totalPages = chapterState.totalPages,
                        onSeekToPage = { targetPage ->
                            scope.launch { readerContext.controller.scrollToPage(targetPage) }
                        },
                        onToggleChapterList = { dispatch(ShowSheet(ReaderSheet.ChapterList)) },
                        onOpenSettings = { dispatch(ShowSheet(ReaderSheet.Settings)) },
                        onOpenReadingMode = { dispatch(ShowSheet(ReaderSheet.ReadingMode)) },
                        onOpenOrientation = { dispatch(ShowSheet(ReaderSheet.Orientation)) },
                    )
                },
                floatingMessage = {
                    if (chapterState.totalPages > 0) {
                        ReaderPageIndicatorBadge(
                            currentPage = currentPage,
                            totalPages = chapterState.totalPages,
                        )
                    }
                },
                sideSheet = {
                    ReaderChapterSideSheet(
                        chapterItems = chapterItems,
                        chapterState = chapterState,
                        uiControlState = uiControlState,
                        dispatch = dispatch,
                    )
                },
                content = {
                    ReaderContentArea(
                        config = config,
                        readerContext = readerContext,
                        pageItems = pageItems,
                        uiControlState = uiControlState,
                        dispatch = dispatch,
                    )
                },
            )
        }
    }
}

@Composable
private fun ReaderTopBar(
    chapterState: ChapterState,
    onBackClick: () -> Unit,
) {
    val title = chapterState.meta?.title ?: "Chapter ${chapterState.order}"
    TopBar(title = { Text(title) }, onBackClick = onBackClick)
}

@Composable
private fun ReaderContentArea(
    config: ReaderConfig,
    readerContext: ReaderContext,
    pageItems: LazyPagingItems<ChapterPage>,
    uiControlState: UiControlState,
    dispatch: (ReaderAction) -> Unit,
) {
    val gestureState = rememberGestureState(config.tapZoneLayout)
    ReaderLayout(
        readerContext = readerContext,
        gestureState = gestureState,
        pageItems = pageItems,
        toggleMenuVisibility = { dispatch(ToggleBarsVisibility) },
        onHideMenu = {
            if (uiControlState.showSystemBars) {
                dispatch(ToggleBarsVisibility)
            }
        },
    )
}

@Composable
private fun ReaderChapterSideSheet(
    chapterItems: LazyPagingItems<Chapter>,
    chapterState: ChapterState,
    uiControlState: UiControlState,
    dispatch: (ReaderAction) -> Unit,
) {
    val isVisible = uiControlState.readerSheet is ReaderSheet.ChapterList
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(animationSpec = tween(), initialOffsetX = { -it }),
        exit = slideOutHorizontally(animationSpec = tween(), targetOffsetX = { -it }),
    ) {
        SideSheetLayout(
            title = { Text("目录") },
            onDismissRequest = { dispatch(HideSheet) },
            closeButton = {
                IconButton(onClick = { dispatch(HideSheet) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭目录")
                }
            },
        ) {
            ChapterList(
                chapters = chapterItems,
                currentChapterId = chapterState.order,
                onChapterClick = { newChapter -> dispatch(JumpToChapter(newChapter)) },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSheetHost(
    sheet: ReaderSheet,
    config: ReaderConfig,
    dispatch: (ReaderAction) -> Unit,
) {
    val onClose = { dispatch(HideSheet) }
    when (sheet) {
        ReaderSheet.ReadingMode -> {
            ReadingModeSelectBottomSheet(
                activeMode = config.readingMode,
                onReadingModeChanged = { dispatch(SetReadingMode(it)) },
                onDismissRequest = onClose,
            )
        }

        ReaderSheet.Orientation -> {
            ScreenOrientationSelectBottomSheet(
                orientation = config.screenOrientation,
                onOrientationChange = { dispatch(SetOrientation(it)) },
                onDismissRequest = onClose,
            )
        }

        ReaderSheet.Settings -> {
            ReadingSettingsBottomSheet(
                config = config,
                dispatch = dispatch,
                onDismissRequest = onClose,
            )
        }

        else -> Unit
    }
}

@Composable
private fun ReaderPageIndicatorBadge(
    currentPage: Int,
    totalPages: Int,
) {
    PageIndicatorBadge(current = currentPage + 1, total = totalPages)
}

@Composable
private fun ReaderBottomBarHost(
    currentPage: Int,
    totalPages: Int,
    readingMode: ReadingMode,
    onSeekToPage: (Int) -> Unit,
    onToggleChapterList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReadingMode: () -> Unit,
    onOpenOrientation: () -> Unit,
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
    )
}

@Composable
private fun PageIndicatorBadge(current: Int, total: Int) {
    Text(
        text = "$current / $total",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}