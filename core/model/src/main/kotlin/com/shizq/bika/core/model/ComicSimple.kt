package com.shizq.bika.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComicSimple(
    @SerialName("_id")
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val totalViews: Int = 0,
    val totalLikes: Int = 0,
    val pagesCount: Int = 0,
    val epsCount: Int = 0,
    val finished: Boolean = false,
    val categories: List<String> = emptyList(),
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