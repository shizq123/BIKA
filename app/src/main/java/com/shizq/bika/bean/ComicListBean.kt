package com.shizq.bika.bean

data class ComicListBean(
        val comics: Comics
    ) {

        data class Comics(
            val docs: List<Doc>,
            val limit: Int,
            val page: Int,
            val pages: Int,
            val total: Int
        ) {
            data class Doc(
                val _id: String,
                val author: String,
                val categories: List<String>,
                val epsCount: Int,
                val finished: Boolean,
                val id: String,
                val likesCount: Int,
                val pagesCount: Int,
                val thumb: Thumb,
                val title: String,
                val totalLikes: Int,
                val totalViews: Int
            ) {

                data class Thumb(
                    val fileServer: String,
                    val originalName: String,
                    val path: String
                )
            }
        }
    }
