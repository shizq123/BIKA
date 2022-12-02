package com.shizq.bika.bean

data class CollectionsBean(
    val collections: List<Collection>
) {

    data class Collection(
        val comics: List<Comic>,
        val title: String
    ) {

        data class Comic(
            val _id: String,
            val author: String,
            val categories: List<String>,
            val epsCount: Int,
            val finished: Boolean,
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