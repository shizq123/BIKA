package com.shizq.bika.bean


data class GameInfoBean(
    val game: Game
) {
    data class Game(
        val adult: Boolean,
        val android: Boolean,
        val androidLinks: List<String>,
        val androidSize: Float,
        val commentsCount: Int,
        val createdAt: String,
        val description: String,
        val downloadsCount: Int,
        val icon: Icon,
        val id: String,
        val ios: Boolean,
        val iosLinks: List<String>,
        val iosSize: Float,
        val isLiked: Boolean,
        val likesCount: Int,
        val publisher: String,
        val screenshots: List<Screenshot>,
        val suggest: Boolean,
        val title: String,
        val updatedAt: String,
        val version: String,
        val videoLink: String,
        val updateContent: String
    ) {
        data class Icon(
            val fileServer: String,
            val originalName: String,
            val path: String
        )

        data class Screenshot(
            val fileServer: String,
            val originalName: String,
            val path: String
        )
    }

}





