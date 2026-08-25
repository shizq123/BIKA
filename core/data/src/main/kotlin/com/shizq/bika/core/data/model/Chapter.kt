package com.shizq.bika.core.data.model

import com.shizq.bika.core.network.model.Episode

data class Chapter(
    val id: String,
    val order: Int,
    val title: String,
    val updatedAt: String
)

fun Episode.asExternalModel() = Chapter(
    id = id,
    order = order,
    title = title,
    updatedAt = updatedAt
)
