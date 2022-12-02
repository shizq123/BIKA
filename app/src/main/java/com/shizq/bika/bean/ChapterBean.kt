package com.shizq.bika.bean

data class ChapterBean(
        val eps: Eps
    ) {

        data class Eps(
            val docs: List<Doc>,
            val limit: Int,
            val page: Int,
            val pages: Int,
            val total: Int
        ) {

            data class Doc(
                val _id: String,
                val id: String,
                val order: Int,
                val title: String,
                val updated_at: String
            )
        }

}