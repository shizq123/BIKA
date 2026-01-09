package com.shizq.bika.core.network.model

import com.shizq.bika.core.model.ComicSimple
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComicResource(
    @SerialName("comics")
    val comics: PageData<ComicSimple>,
)

@Serializable
data class ComicSearchResponseData(
    @SerialName("comics")
    val comics: PageData<ComicInSearch>,
)

@Serializable
data class ComicInSearch(
    @SerialName("id")
    val id: String,
    val author: String,
    val categories: List<String>,
    val chineseTeam: String,
    val createdAt: String,
    val description: String,
    val finished: Boolean,
    val likesCount: Int,
    val tags: List<String>,
    val thumb: Image,
    val title: String,
    val totalViews: Int? = null,
    val updatedAt: String,
)
// totalViews, totalLikes, pagesCount, epsCount