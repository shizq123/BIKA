package com.shizq.bika.core.data.model

import kotlin.time.Instant

data class ReadingProgress(
    val chapterIndex: Int,
    val pageIndex: Int,
    val groupIndex: Int? = null
)

data class ReadChapterIdentifier(
    val chapterIndex: Int,
    val groupIndex: Int? = null
)

data class History(
    val id: String,
    val title: String,
    val author: String,
    val cover: String,
    val lastReadAt: Instant,
    val lastReadProgress: ReadingProgress,
    val readChapters: Set<ReadChapterIdentifier>,
    val maxPage: Int?
)

//fun HistoryWithReadChapters.asExternalModel(): History = History(
//    id = history.id,
//    title = history.title,
//    author = history.author,
//    cover = history.cover,
//    lastReadAt = history.lastReadAt,
//    maxPage = history.maxPage,
//    lastReadProgress = ReadingProgress(
//        chapterIndex = history.lastReadProgress.chapterIndex,
//        pageIndex = history.lastReadProgress.pageIndex,
//        groupIndex = history.lastReadProgress.groupIndex
//    ),
//    readChapters = readChapters.map {
//        ReadChapterIdentifier(
//            chapterIndex = it.chapterIndex,
//            groupIndex = it.groupIndex
//        )
//    }.toSet()
//)