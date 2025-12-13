package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChapterPagesData(
    @SerialName("pages")
    val paginationData: PaginationData,


    @SerialName("ep")
    val chapterInfo: ChapterInfo
)

@Serializable
data class PaginationData(
    @SerialName("docs")
    val images: List<Image>,

    val total: Int,

    val limit: Int,

    @SerialName("page")
    val currentPageIndex: Int,

    @SerialName("pages")
    val totalPages: Int
)

@Serializable
data class Image(
    @SerialName("_id")
    val imageId: String,

    val media: Media
)

@Serializable
data class ChapterInfo(
    @SerialName("_id")
    val chapterId: String,

    @SerialName("title")
    val title: String
)