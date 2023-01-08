package com.shizq.bika.bean

data class CategoriesBean(
    val categories: List<Category>
) {

    data class Category(
        val _id: String,
        val active: Boolean,
        val description: String,
        val isWeb: Boolean,
        val link: String,
        val thumb: Thumb,
        var title: String,
        var imageRes: Int?
    ) {

        data class Thumb(
            val fileServer: String,
            val originalName: String,
            val path: String
        )
    }
}