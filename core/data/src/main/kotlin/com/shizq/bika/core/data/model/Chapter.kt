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
 * 漫画的完整章节目录（按 [Chapter.order] 升序排列，第1话在最前，最新话在末尾）。
 *
 * 用于上下章导航的相邻章节查询：目录需要支持随机访问任意 order，
 * 分页窗口无法满足这一诉求。
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
 * 上下章导航信息：给定当前章节 order，解析出相邻的前后章节。
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

fun ChapterCatalog.navigationAt(order: Int): ChapterNavigation {
    val currentIndex = chapters.indexOfFirst { it.order == order }
    val prev = if (currentIndex >= 0) {
        chapters.getOrNull(currentIndex - 1)
    } else {
        // 当前章节未在列表中时的兜底：order 较小者为上一章
        chapters.lastOrNull { it.order < order }
    }
    val next = if (currentIndex >= 0) {
        chapters.getOrNull(currentIndex + 1)
    } else {
        // 当前章节未在列表中时的兜底：order 较大者为下一章
        chapters.firstOrNull { it.order > order }
    }
    return ChapterNavigation(
        prev = prev,
        next = next,
        isResolved = isComplete,
    )
}
