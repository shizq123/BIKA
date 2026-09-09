package com.shizq.bika.core.network.model

import com.shizq.bika.core.network.utils.LenientIntSerializer
import kotlinx.serialization.Serializable

@Serializable
data class PageData<T>(
    @Serializable(with = LenientIntSerializer::class) val total: Int,
    @Serializable(with = LenientIntSerializer::class) val limit: Int,
    @Serializable(with = LenientIntSerializer::class) val page: Int,
    @Serializable(with = LenientIntSerializer::class) val pages: Int,
    val docs: List<T>,
)
