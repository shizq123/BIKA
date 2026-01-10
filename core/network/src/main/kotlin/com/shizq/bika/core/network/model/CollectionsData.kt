package com.shizq.bika.core.network.model

import com.shizq.bika.core.model.ComicSimple
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CollectionsData(
    @SerialName("collections") val collections: List<CollectionItem>
)

@Serializable
data class CollectionItem(
    @SerialName("comics") val comics: List<ComicSimple>
)