package com.shizq.bika.ui.reader

import androidx.compose.runtime.Composable
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.ui.reader.bar.ReaderBottomBar

@Composable
internal fun ReaderBottomBarHost(
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
