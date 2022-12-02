package com.shizq.bika.bean

data class PicaInitBean(
    val code: Int,
    val `data`: Data,
    val error: String,
    val message: String
) {
    data class Data(
        val apiLevel: Int,
        val categories: List<Category>,
        val imageServer: String,
        val isIdUpdated: Boolean,
        val isPunched: Boolean,
        val latestApplication: LatestApplication,
        val minApiLevel: Int,
        val notification: Any
    ) {
        data class Category(
            val _id: String,
            val title: String
        )

        data class LatestApplication(
            val _id: String,
            val apk: Apk,
            val created_at: String,
            val downloadUrl: String,
            val updateContent: String,
            val updated_at: String,
            val version: String
        ) {
            data class Apk(
                val fileServer: String,
                val originalName: String,
                val path: String
            )
        }
    }


}

