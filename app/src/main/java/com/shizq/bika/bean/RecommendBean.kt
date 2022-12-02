package com.shizq.bika.bean

data class RecommendBean(
        val comics: List<Comic>
    ) {

        data class Comic(
            val _id: String,
            val author: String,
            val categories: List<String>,
            val epsCount: Int,
            val finished: Boolean,
            val likesCount: Int,
            val pagesCount: Int,
            val thumb: Thumb,
            val title: String
        ) {

            data class Thumb(
                val fileServer: String,
                val originalName: String,
                val path: String
            )
        }
    }
