package com.shizq.bika.bean

data class CommentsBean(
    val comments: Comments,
    val topComments: List<TopComment>
) {
    data class Comments(
        var docs: List<Doc>,
        val limit: Int,
        val page: Int,
        val pages: Int,
        val total: Int
    ) {
        data class Doc(
            var _id: String,
            var _user: User,
            var commentsCount: Int,
            var content: String,
            var created_at: String,
            var hide: Boolean,
            var id: String,
            var isLiked: Boolean,
            var isTop: Boolean,
            var likesCount: Int,
            var totalComments: Int,
            var isMainComment: Boolean
        )
    }

    data class TopComment(
        val _id: String,
        val _user: User,
        val commentsCount: Int,
        val content: String,
        val created_at: String,
        val hide: Boolean,
        val isLiked: Boolean,
        val isTop: Boolean,
        val likesCount: Int,
        val totalComments: Int
    )
    data class User(
        val _id: String,
        val avatar: Avatar,
        val character: String,
        val characters: List<Any>,
        val exp: Int,
        val gender: String,
        val level: Int,
        val name: String,
        val role: String,
        val slogan: String,
        val title: String,
        val verified: Boolean
    ) {
        data class Avatar(
            val fileServer: String,
            val originalName: String,
            val path: String
        )
    }
}









