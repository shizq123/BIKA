package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentsData(
    @SerialName("comments")
    val comments: PageData<CommentData>,

    @SerialName("topComments")
    val topComments: List<CommentData> = emptyList()
)

@Serializable
data class CommentData(
    @SerialName("_id")
    val id: String,

    @SerialName("content")
    val content: String,

    @SerialName("_user")
    val user: UserData = UserData(),

    @SerialName("totalComments")
    val totalComments: Int = 0,

    @SerialName("isTop")
    val isTop: Boolean = false,

    @SerialName("hide")
    val hide: Boolean = false,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("likesCount")
    val likesCount: Int,

    @SerialName("commentsCount")
    val commentsCount: Int = 0,

    @SerialName("isLiked")
    val isLiked: Boolean
)