package com.shizq.bika.core.network.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComicData(
    @SerialName("comic")
    val comic: Comic = Comic()
) {
    @Serializable
    data class Comic(
        @SerialName("allowComment")
        val allowComment: Boolean = false,
        @SerialName("allowDownload")
        val allowDownload: Boolean = false,
        @SerialName("author")
        val author: String = "",
        @SerialName("categories")
        val categories: List<String> = listOf(),
        @SerialName("chineseTeam")
        val chineseTeam: String = "",
        @SerialName("commentsCount")
        val commentsCount: Int = 0,
        @SerialName("created_at")
        val createdAt: String = "",
        @SerialName("_creator")
        val creator: Creator = Creator(),
        @SerialName("description")
        val description: String = "",
        @SerialName("epsCount")
        val epsCount: Int = 0,
        @SerialName("finished")
        val finished: Boolean = false,
        @SerialName("_id")
        val id: String = "",
        @SerialName("isFavourite")
        val isFavourite: Boolean = false,
        @SerialName("isLiked")
        val isLiked: Boolean = false,
        @SerialName("likesCount")
        val likesCount: Int = 0,
        @SerialName("pagesCount")
        val pagesCount: Int = 0,
        @SerialName("tags")
        val tags: List<String> = listOf(),
        @SerialName("thumb")
        val thumb: Media = Media(),
        @SerialName("title")
        val title: String = "",
        @SerialName("totalComments")
        val totalComments: Int = 0,
        @SerialName("totalLikes")
        val totalLikes: Int = 0,
        @SerialName("totalViews")
        val totalViews: Int = 0,
        @SerialName("updated_at")
        val updatedAt: String = "",
        @SerialName("viewsCount")
        val viewsCount: Int = 0
    ) {
        @Serializable
        data class Creator(
            @SerialName("avatar")
            val avatar: Media = Media(),
            @SerialName("characters")
            val characters: List<String> = listOf(),
            @SerialName("exp")
            val exp: Int = 0,
            @SerialName("gender")
            val gender: String = "",
            @SerialName("_id")
            val id: String = "",
            @SerialName("level")
            val level: Int = 0,
            @SerialName("name")
            val name: String = "",
            @SerialName("role")
            val role: String = "",
            @SerialName("slogan")
            val slogan: String = "",
            @SerialName("title")
            val title: String = ""
        )
    }
}