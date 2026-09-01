package com.shizq.bika.core.data.model

import com.shizq.bika.core.network.model.Episode

data class Chapter(
    val id: String,
    val order: Int,
    val title: String,
    val updatedAt: String
)

fun Episode.asExternalModel() = Chapter(
    id = id,
    order = order,
    title = title,
    updatedAt = updatedAt
)

/**
 * 漫画的完整章节目录（按 [Chapter.order] 升序排列）。
 *
 * 用于上下章导航的相邻章节查询：目录需要支持随机访问任意 order，
 * 分页窗口（[ChapterListPagingSource]）无法满足这一诉求。
 *
 * @param isComplete 是否已拉取完整目录。为 false 时 [navigationAt] 给出的
 *   相邻章节仅代表“已知范围内”的结果，不能用于判断“是否为第一章/最后一章”。
 */
data class ChapterCatalog(
    val chapters: List<Chapter>,
    val isComplete: Boolean,
) {
    companion object {
        val Empty = ChapterCatalog(chapters = emptyList(), isComplete = false)
    }
}

/**
 * 上下章导航信息：给定当前章节 order，得到最近的更小/更大 order 的章节。
 *
 * 用“最近的更小/更大 order”而非“索引 ±1”，是因为当前章节可能不在目录中
 * （例如仅下载模式下打开了一个未下载完成的章节），此时索引法会直接失效，
 * 而按 order 比较仍能给出正确的相邻项。
 */
data class ChapterNavigation(
    val prev: Chapter?,
    val next: Chapter?,
    val isResolved: Boolean,
) {
    companion object {
        val Unresolved = ChapterNavigation(prev = null, next = null, isResolved = false)
    }
}

fun ChapterCatalog.navigationAt(order: Int): ChapterNavigation = ChapterNavigation(
    prev = chapters.lastOrNull { it.order < order },
    next = chapters.firstOrNull { it.order > order },
    isResolved = isComplete,
)
