package com.shizq.bika.core.network.model

import com.shizq.bika.core.network.utils.IntAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.w3c.dom.Comment

@Serializable
data class CommentsData(
    @SerialName("comments")
    val comments: CommentsPage,

    @SerialName("topComments")
    val topComments: List<Comment> = emptyList()
)

@Serializable
data class CommentsPage(
    @SerialName("docs")
    val docs: List<Comment>,

    @SerialName("total")
    val total: Int,

    @SerialName("limit")
    val limit: Int,

    @SerialName("page")
    @Serializable(with = IntAsStringSerializer::class)
    val page: Int,

    @SerialName("pages")
    val pages: Int
)

@Serializable
data class Comment(
    @SerialName("_id")
    val id: String,

    @SerialName("content")
    val content: String,

    @SerialName("_user")
    val user: UserData,

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
    val commentsCount: Int,

    @SerialName("isLiked")
    val isLiked: Boolean
)