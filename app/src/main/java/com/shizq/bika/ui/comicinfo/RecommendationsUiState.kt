package com.shizq.bika.ui.comicinfo

import com.shizq.bika.core.network.model.RecommendComicDto
import com.shizq.bika.core.network.model.RecommendationData

sealed interface RecommendationsUiState {
    data class Success(val comics: List<ComicSummary>) : RecommendationsUiState
    data class Error(val message: String) : RecommendationsUiState
    object Loading : RecommendationsUiState
}

data class ComicSummary(
    val id: String,
    val title: String,
    val coverUrl: String,
    val author: String
)

fun RecommendComicDto.toComicSummary(): ComicSummary {
    return ComicSummary(
        id = this.id,
        title = this.title,
        coverUrl = this.thumb.fileServer + "/static/" + this.thumb.path,
        author = this.author
    )
}

fun RecommendationData.toComicSummaryList(): List<ComicSummary> {
    return this.comics.map { it.toComicSummary() }
}