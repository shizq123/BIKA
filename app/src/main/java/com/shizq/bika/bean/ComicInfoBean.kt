package com.shizq.bika.bean

data class ComicInfoBean(
    val comic: Comic
) {

    data class Comic(
        val _creator: Creator,
        val _id: String,
        val allowComment: Boolean,
        val allowDownload: Boolean,
        val author: String,
        val categories: List<String>,
        val chineseTeam: String,
        val commentsCount: Int,
        val created_at: String,
        val description: String,
        val epsCount: Int,
        val finished: Boolean,
        val isFavourite: Boolean,
        val isLiked: Boolean,
        val likesCount: Int,
        val pagesCount: Int,
        val tags: List<String>,
        val thumb: Thumb,
        val title: String,
        val totalComments: Int,
        val totalLikes: Int,
        val totalViews: Int,
        val updated_at: String,
        val viewsCount: Int
    ) {
        data class Creator(
            val _id: String,
            val avatar: Avatar,
            val characters: List<String>,
            val exp: Int,
            val gender: String,
            val level: Int,
            val name: String,
            val role: String,
            val slogan: String,
            val title: String,
            val character: String,
            val verified: Boolean
        )

        data class Thumb(
            val fileServer: String,
            val originalName: String,
            val path: String
        )

        data class Avatar(
            val fileServer: String,
            val originalName: String,
            val path: String
        )


    }

}