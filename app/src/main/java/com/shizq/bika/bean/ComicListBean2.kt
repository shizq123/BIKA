package com.shizq.bika.bean

//漫画列表第二种数据类型
data class ComicListBean2(
        val comics: List<Comics>
    ){

        data class Comics(
            val _id: String,
            val title: String,
            val author: String,
            val pagesCount: Int,
            val epsCount: Int,
            val finished: Boolean,
            val thumb: Thumb,
            val totalViews: Int,
            val totalLikes: Int,
            val likesCount: Int,
            val viewsCount: Int,
            val leaderboardCount: Int,
            val categories: List<String>
        ){

            data class Thumb(
                val fileServer: String,
                val path: String,
                val originalName: String
            )
        }
    }

