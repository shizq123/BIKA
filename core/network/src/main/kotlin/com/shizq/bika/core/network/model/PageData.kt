package com.shizq.bika.core.network.model

import com.shizq.bika.core.network.utils.FuzzyIntSerializer
import kotlinx.serialization.Serializable

@Serializable
data class PageData<T>(
    @Serializable(with = FuzzyIntSerializer::class) val total: Int,
    @Serializable(with = FuzzyIntSerializer::class) val limit: Int,
    @Serializable(with = FuzzyIntSerializer::class) val page: Int,
    @Serializable(with = FuzzyIntSerializer::class) val pages: Int,
    val docs: List<T>,
)
