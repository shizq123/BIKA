package com.shizq.bika.core.data.model

import com.shizq.bika.core.network.model.ComicDto

data class Comic(
    val id: String,
    val title: String,
    val author: String,
    val pagesCount: Int,
    val epsCount: Int,
    val finished: Boolean,
    val categories: List<String>,
    val tags: List<String>,
    val totalLikes: Int,
    val totalViews: Int,
    val coverUrl: String
)

fun ComicDto.asExternalModel() = Comic(
    id = id,
    title = title,
    author = author,
    pagesCount = pagesCount,
    epsCount = epsCount,
    finished = finished,
    categories = categories,
    tags = tags,
    totalLikes = totalLikes,
    totalViews = totalViews,
    coverUrl = thumb.originalImageUrl
)