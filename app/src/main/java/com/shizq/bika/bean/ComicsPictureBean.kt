package com.shizq.bika.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames


data class ComicsPictureBean(
    val pages: Pages,
    val ep: Ep
) {
    data class Pages(
        val total: Int,
        val limit: Int,
        val page: Int,
        val pages: Int,
        val docs: List<Docs>
    ) {
        data class Docs(
            val _id: String,
            val media: Media,
            val id: String
        ) {
            data class Media(
                val originalName: String,
                val path: String,
                val fileServer: String
            )
        }
    }

    data class Ep(
        val _id: String,
        val title: String
    )
}

@Serializable
data class ComicEpisodeData(
    @SerialName("ep")
    val ep: Ep = Ep(),
    @SerialName("pages")
    val pages: Pages = Pages()
) {
    @Serializable
    data class Ep(
        @SerialName("_id")
        val id: String = "",
        @SerialName("title")
        val title: String = ""
    )

    @Serializable
    data class Pages(
        @SerialName("docs")
        val docs: List<Doc> = listOf(),
        @SerialName("limit")
        val limit: Int = 0,
        @SerialName("page")
        val page: Int = 0,
        @SerialName("pages")
        val pages: Int = 0,
        @SerialName("total")
        val total: Int = 0
    ) {
        @Serializable
        data class Doc(
            @JsonNames("_id", "id")
            val id: String = "",
            @SerialName("media")
            val media: Media = Media()
        )
    }
}