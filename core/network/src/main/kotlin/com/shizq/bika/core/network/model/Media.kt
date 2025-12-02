package com.shizq.bika.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class Media(
    val originalName: String = "",
    val path: String = "",
    val fileServer: String = "",
) {
    val imageUrl: String
        get() = "https://s3.picacomic.com/static/${path}"
    val originalImageUrl: String
        get() = "${fileServer}/static/${path}"
}