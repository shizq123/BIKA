package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CollectionsData(
    @SerialName("collections") val collections: List<CollectionItem>
)

@Serializable
data class CollectionItem(
    @SerialName("title") val title: String,
    @SerialName("comics") val comics: List<ComicDto>
)

@Serializable
data class ComicDto(
    @SerialName("_id") val id: String,

    @SerialName("title") val title: String,
    @SerialName("author") val author: String,
    @SerialName("pagesCount") val pagesCount: Int,
    @SerialName("epsCount") val epsCount: Int,
    @SerialName("finished") val finished: Boolean,
    @SerialName("categories") val categories: List<String>,
    @SerialName("tags") val tags: List<String>,
    @SerialName("totalLikes") val totalLikes: Int,
    @SerialName("totalViews") val totalViews: Int,
    @SerialName("thumb") val thumb: Media
)