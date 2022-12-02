package com.shizq.bika.bean

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

