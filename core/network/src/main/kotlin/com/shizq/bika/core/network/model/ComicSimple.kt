package com.shizq.bika.core.network.model

import kotlinx.serialization.Serializable

@Serializable
class ComicSimple(
    val id: String,
    val title: String,
    val author: String,
    val pagesCount: Int,
    val epsCount: Int,
    val finished: Boolean,
    val categories: List<String>,
    val image: Image2,
    val likesCount: Int,
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