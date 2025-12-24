package com.shizq.bika.core.data.model

import com.shizq.bika.core.network.model.CommentData

data class Comment(
    val id: String,
    val content: String,
    val user: User,
    val totalComments: Int = 0,
    val createdAt: String,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean
)

fun CommentData.asExternalModel() = Comment(
    id = id,
    content = content,
    user = user.asExternalModel(),
    totalComments = totalComments,
    createdAt = createdAt,
    likesCount = likesCount,
    commentsCount = commentsCount,
    isLiked = isLiked
)