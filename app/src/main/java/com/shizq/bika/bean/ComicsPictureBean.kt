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
data class ffff(
    @SerialName("code")
    val code: Int = 0,
    @SerialName("data")
    val `data`: Data = Data(),
    @SerialName("message")
    val message: String = ""
) {
    @Serializable
    data class Data(
        @SerialName("comics")
        val comics: Comics = Comics()
    ) {
        @Serializable
        data class Comics(
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
                @SerialName("author")
                val author: String = "",
                @SerialName("categories")
                val categories: List<String> = listOf(),
                @SerialName("epsCount")
                val epsCount: Int = 0,
                @SerialName("finished")
                val finished: Boolean = false,
                @JsonNames("_id", "id")
                val id: String = "",
                @SerialName("likesCount")
                val likesCount: Int = 0,
                @SerialName("pagesCount")
                val pagesCount: Int = 0,
                @SerialName("tags")
                val tags: List<String> = listOf(),
                @SerialName("thumb")
                val media: Media = Media(),
                @SerialName("title")
                val title: String = "",
                @SerialName("totalLikes")
                val totalLikes: Int = 0,
                @SerialName("totalViews")
                val totalViews: Int = 0
            )
        }
    }
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