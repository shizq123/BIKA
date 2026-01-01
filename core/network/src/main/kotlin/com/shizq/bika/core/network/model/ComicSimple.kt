package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class ComicResource(
    @SerialName("comics")
    val comics: PageData<ComicSimple>,
)
@Serializable
data class ComicSimple(
    @JsonNames("_id", "id")
    val id: String,
    val title: String,
    val author: String,
    val totalViews: Int,
    val totalLikes: Int,
    val pagesCount: Int,
    val epsCount: Int,
    val finished: Boolean,
    val categories: List<String>,
    val tags: List<String> = emptyList(),
    @SerialName("thumb")
    val image: Image2,
)

@Serializable
data class Image2(
    val originalName: String = "",
    val path: String = "",
    val fileServer: String = "",
) {
    val originalImageUrl: String
        get() = "${fileServer}/static/${path}"
}