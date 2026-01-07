package com.shizq.bika.core.network.model

import com.shizq.bika.core.model.ComicSimple
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComicResource(
    @SerialName("comics")
    val comics: PageData<ComicSimple>,
)