package com.shizq.bika.ui.reader

import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.context.findActivity
import com.shizq.bika.core.model.ScreenOrientation
import com.shizq.bika.core.ui.composition.LocalWindow
import com.shizq.bika.paging.ChapterPage
import com.shizq.bika.ui.reader.layout.ReaderConfig
import com.shizq.bika.ui.reader.layout.ReaderController
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.state.SeekState
import com.shizq.bika.ui.reader.state.UiControlState
import com.shizq.bika.ui.reader.util.preload.ChapterPagePreloadProvider
import com.shizq.bika.ui.reader.util.preload.PagingPreload
import com.shizq.bika.ui.reader.util.preload.ScrollStateProvider
import kotlinx.coroutines.flow.debounce

@Composable
fun ReaderSideEffects(
    config: ReaderConfig,
    uiControl: UiControlState,
    controller: ReaderController,
    chapterPages: LazyPagingItems<ChapterPage>,
    scrollStateProvider: ScrollStateProvider,
    preloadCount: Int,
    dispatch: (ReaderAction) -> Unit,
) {
    val context = LocalContext.current

    SystemUiController(showSystemUI = uiControl.showSystemBars)
    KeepScreenOnEffect()
    OrientationEffect(config.screenOrientation)

    ReaderSeekEffect(
        seekState = uiControl.seekState,
        controller = controller,
        dispatch = dispatch,
    )

    ReaderProgressEffect(
        controller = controller,
        dispatch = dispatch,
    )

    val preloadModelProvider = remember(context) {
        ChapterPagePreloadProvider(context)
    }
    PagingPreload(
        pagingItems = chapterPages,
        scrollStateProvider = scrollStateProvider,
        modelProvider = preloadModelProvider,
        preloadCount = preloadCount,
    )
}

@Composable
private fun ReaderSeekEffect(
    seekState: SeekState,
    controller: ReaderController,
    dispatch: (ReaderAction) -> Unit,
) {
    LaunchedEffect(seekState) {
        if (seekState is SeekState.Seeking) {
            controller.scrollToPage(seekState.targetPage.toInt())
            dispatch(ReaderAction.SeekConsumed)
        }
    }
}

@Composable
private fun ReaderProgressEffect(
    controller: ReaderController,
    dispatch: (ReaderAction) -> Unit,
) {
    LaunchedEffect(controller) {
        controller.visibleItemIndex
            .debounce(1000)
            .collect { index ->
                dispatch(ReaderAction.SyncReadingProgress(index))
            }
    }
}

@Composable
private fun OrientationEffect(orientation: ScreenOrientation) {
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
private fun KeepScreenOnEffect() {
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
