package com.shizq.bika.ui.comicinfo

import androidx.compose.runtime.Immutable
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.network.model.ComicData
import com.shizq.bika.core.network.model.ComicData.Comic.Creator
import com.shizq.bika.core.network.model.RecommendComicDto
import com.shizq.bika.core.network.model.RecommendationData

sealed interface UnitedDetailsUiState {
    data object Initialize : UnitedDetailsUiState

    @Immutable
    data class Content(
        val id: String,
        val detail: ComicDetail = ComicDetail(),
        val recommendations: List<ComicSummary> = emptyList(),
        /** 置顶评论。由 CommentPagingSource 通过 TopCommentsLoaded 派发写入 */
        val pinnedComments: List<Comment> = emptyList(),
        /**
         * 正在查看回复的根评论，null 表示回复弹窗关闭。
         *
         * 存整个 Comment 而不是 id：弹窗需要展示根评论的作者、内容、点赞数，
         * 只存 id 的话 UI 还得自己留一份映射，又会变成第二个真相源。
         */
        val viewingReplies: Comment? = null
    ) : UnitedDetailsUiState

    data class Error(val cause: Throwable) : UnitedDetailsUiState
}

fun ComicData.toComicDetail(): ComicDetail {
    return ComicDetail(
        author = comic.author,
        categories = comic.categories,
        chineseTeam = comic.chineseTeam,
        commentsCount = comic.commentsCount,
        createdAt = comic.createdAt,
        creator = comic.creator,
        description = comic.description,
        epsCount = comic.epsCount,
        finished = comic.finished,
        id = comic.id,
        isFavourited = comic.isFavourite,
        isLiked = comic.isLiked,
        pagesCount = comic.pagesCount,
        tags = comic.tags,
        cover = comic.thumb.originalImageUrl,
        title = comic.title,
        totalLikes = comic.totalLikes,
        totalViews = comic.totalViews,
        updatedAt = comic.updatedAt
    )
}

data class ComicDetail(
    val author: String = "",
    val categories: List<String> = listOf(),
    val chineseTeam: String = "",
    val commentsCount: Int = 0,
    val createdAt: String = "",
    val creator: Creator = Creator(),
    val description: String = "",
    val epsCount: Int = 0,
    val finished: Boolean = false,
    val id: String = "",
    val isFavourited: Boolean = false,
    val isLiked: Boolean = false,
    val pagesCount: Int = 0,
    val tags: List<String> = listOf(),
    val cover: String = "",
    val title: String = "",
    val totalLikes: Int = 0,
    val totalViews: Int = 0,
    val updatedAt: String = "",
)

data class ComicSummary(
    val id: String,
    val title: String,
    val coverUrl: String,
    val author: String
)

fun RecommendComicDto.toComicSummary(): ComicSummary {
    return ComicSummary(
        id = id,
        title = title,
        coverUrl = thumb.originalImageUrl,
        author = author
    )
}

fun RecommendationData.toComicSummaryList(): List<ComicSummary> {
    return this.comics.map { it.toComicSummary() }
}