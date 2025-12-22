package com.shizq.bika.ui.reader.state

import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.core.model.ScreenOrientation
import com.shizq.bika.paging.Chapter
import com.shizq.bika.paging.ChapterMeta
import com.shizq.bika.ui.reader.layout.ReaderConfig

sealed interface ReaderUiState {
    data object Initializing : ReaderUiState

    data class Ready(
        val id: String,
        val chapter: ChapterState,
        val config: ReaderConfig = ReaderConfig.Default,

        val overlay: ReaderOverlayState = ReaderOverlayState(),
        val activeSheet: ActiveSheet = ActiveSheet.None,
        val error: Throwable? = null
    ) : ReaderUiState
}

data class ChapterState(
    val order: Int,
    val meta: ChapterMeta? = null,
    val totalPages: Int = 0,
    val isLoading: Boolean = false
)

/**
 * @param isMenuVisible 是否显示菜单
 * @param isChapterListVisible 是否显示章节列表
 * @param isSettingsVisible 是否显示设置
 */
data class ReaderOverlayState(
    val isMenuVisible: Boolean = false,
    val isChapterListVisible: Boolean = false,
    val isSettingsVisible: Boolean = false,
    val seekState: SeekState = SeekState.Idle
)

sealed interface SeekState {
    data object Idle : SeekState
    data class Seeking(val progress: Float) : SeekState
}

sealed interface ReaderAction {
    data class ChangeChapter(val chapter: Chapter) : ReaderAction
    data class ChangeReadingMode(val mode: ReadingMode) : ReaderAction
    data class ChangeOrientation(val orientation: ScreenOrientation) : ReaderAction
    data class SaveProgress(val pageIndex: Int) : ReaderAction

    data object ToggleSettings : ReaderAction
    data object ToggleMenu : ReaderAction
    data object ToggleChapterList : ReaderAction

    data class OnMetaLoaded(val meta: ChapterMeta) : ReaderAction
    data class LoadHistory(val id: String, val chapterOrder: Int) : ReaderAction

    data class OpenSheet(val sheet: ActiveSheet) : ReaderAction
}