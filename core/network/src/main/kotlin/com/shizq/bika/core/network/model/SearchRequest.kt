package com.shizq.bika.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchRequest(
    val keyword: String,
    val sort: String,
    val categories: List<String>
)