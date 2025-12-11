package com.shizq.bika.core.data.model

data class Comic(
    val id: String,
    val title: String,
    val author: String,
    val pagesCount: Int,
    val epsCount: Int,
    val finished: Boolean,
    val categories: List<String>,
    val tags: List<String>,
    val totalLikes: Int,
    val totalViews: Int,
    val coverUrl: String
)