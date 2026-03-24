package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class CommentDoc(
    @SerialName("comments")
    val comments: PageData<Comment>
)

@Serializable
data class Comment(
    @SerialName("_comic")
    val comic: ComicMiniInfo = ComicMiniInfo(),
    @SerialName("commentsCount")
    val commentsCount: Int = 0,
    @SerialName("content")
    val content: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("hide")
    val hide: Boolean = false,
    @JsonNames("_id", "id")
    val id: String = "",
    @SerialName("isLiked")
    val isLiked: Boolean = false,
    @SerialName("likesCount")
    val likesCount: Int = 0,
    @SerialName("totalComments")
    val totalComments: Int = 0
) {
    @Serializable
    data class ComicMiniInfo(
        @SerialName("_id")
        val id: String = "",
        @SerialName("title")
        val title: String = ""
    )
}