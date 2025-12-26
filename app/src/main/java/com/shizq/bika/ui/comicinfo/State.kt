package com.shizq.bika.ui.comicinfo

import androidx.paging.PagingData
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.network.model.ComicData
import com.shizq.bika.core.network.model.ComicData.Comic.Creator
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.core.network.model.RecommendComicDto
import com.shizq.bika.core.network.model.RecommendationData

sealed interface UnitedDetailsUiState {
    data object I : UnitedDetailsUiState
    data class Content(
        val id: String,
        val detail: ComicDetail? = null,
        val recommendations: List<ComicSummary> = emptyList(),
        val pinnedComments: List<Comment> = emptyList(),
        val episodes: PagingData<Episode> = PagingData.empty(),
        val regularComments: PagingData<Comment> = PagingData.empty(),
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
    val chineseTeam: String? = null,
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
        coverUrl = thumb.originalName,
        author = author
    )
}

fun RecommendationData.toComicSummaryList(): List<ComicSummary> {
    return this.comics.map { it.toComicSummary() }
}