package com.shizq.bika.ui.reader.state

import androidx.compose.runtime.Immutable
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.core.model.ScreenOrientation
import com.shizq.bika.core.model.TapZoneLayout
import com.shizq.bika.paging.Chapter
import com.shizq.bika.paging.ChapterMeta
import com.shizq.bika.ui.reader.layout.ReaderConfig

sealed interface ReaderUiState {
    data object Initializing : ReaderUiState

    @Immutable
    data class Ready(
        val id: String,
        val chapter: ChapterState,
        val config: ReaderConfig = ReaderConfig.Default,
        val uiControl: UiControlState = UiControlState(),
        val error: Throwable? = null
    ) : ReaderUiState {
        val isOverlayActive: Boolean
            get() = uiControl.readerSheet != ReaderSheet.None || uiControl.showSystemBars
    }
}

@Immutable
data class ChapterState(
    val order: Int,
    val meta: ChapterMeta? = null,
    val totalPages: Int = 0,
    val isLoading: Boolean = false
)

/**
 * @param showSystemBars 是否显示系统栏/顶部/底部工具栏
 * @param readerSheet 当前激活的浮层 (BottomSheet 或 SideSheet)
 * @param seekState 进度条拖拽状态
 */
@Immutable
data class UiControlState(
    val showSystemBars: Boolean = false,
    val readerSheet: ReaderSheet = ReaderSheet.None,
    val seekState: SeekState = SeekState.Idle
)

sealed interface SeekState {
    data object Idle : SeekState
    data class Seeking(val targetPage: Float) : SeekState
}

sealed interface ReaderAction {
    data class JumpToChapter(val chapter: Chapter) : ReaderAction
    data class SyncReadingProgress(val pageIndex: Int) : ReaderAction

    data object ToggleBarsVisibility : ReaderAction
    data class ShowSheet(val sheet: ReaderSheet) : ReaderAction
    data object HideSheet : ReaderAction

    data class SetReadingMode(val mode: ReadingMode) : ReaderAction
    data class SetOrientation(val orientation: ScreenOrientation) : ReaderAction
    data class SetPreloadCount(val count: Int) : ReaderAction
    data class SetTapZoneLayout(val layout: TapZoneLayout) : ReaderAction
    data class SetVolumeKeyNavigation(val enable: Boolean) : ReaderAction

    data class ChapterMetaLoaded(val meta: ChapterMeta) : ReaderAction

    data class Initialize(val id: String, val chapterOrder: Int) : ReaderAction
}