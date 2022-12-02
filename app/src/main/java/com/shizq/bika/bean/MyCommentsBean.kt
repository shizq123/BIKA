package com.shizq.bika.bean

data class MyCommentsBean(
    val comments: Comments
) {
    data class Comments(
        val docs: List<Doc>,
        val limit: Int,
        val page: String,
        val pages: Int,
        val total: Int
    ) {
        data class Doc(
            val _comic: Comic,
            val commentsCount: Int,
            val content: String,
            val created_at: String,
            val _game: Game,
            val hide: Boolean,
            val _id: String,
            val id: String,
            var isLiked: Boolean,
            var likesCount: Int,
            val totalComments: Int
        ) {
            data class Comic(
                val _id: String,
                val title: String
            )

            data class Game(
                val _id: String,
                val title: String
            )
        }
    }
}





