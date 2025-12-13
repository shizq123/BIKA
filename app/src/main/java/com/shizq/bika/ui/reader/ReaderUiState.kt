package com.shizq.bika.ui.reader

import com.shizq.bika.paging.ChapterMeta
import com.shizq.bika.ui.reader.layout.ReaderConfig

data class ReaderUiState(
    val config: ReaderConfig = ReaderConfig.Default,
    val currentChapterOrder: Int = 1,
    val chapterMeta: ChapterMeta? = null,
)