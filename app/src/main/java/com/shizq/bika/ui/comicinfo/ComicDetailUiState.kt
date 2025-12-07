package com.shizq.bika.ui.comicinfo

import com.shizq.bika.core.network.model.ComicData
import com.shizq.bika.core.network.model.ComicData.Comic.Creator

sealed interface ComicDetailUiState {
    data class Success(val detail: ComicDetail) : ComicDetailUiState
    data object Loading : ComicDetailUiState
    data class Error(val message: String) : ComicDetailUiState
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
        isFavourite = comic.isFavourite,
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
    val isFavourite: Boolean = false,
    val isLiked: Boolean = false,
    val pagesCount: Int = 0,
    val tags: List<String> = listOf(),
    val cover: String,
    val title: String = "",
    val totalLikes: Int = 0,
    val totalViews: Int = 0,
    val updatedAt: String = "",
)