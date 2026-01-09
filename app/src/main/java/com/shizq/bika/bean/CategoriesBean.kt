package com.shizq.bika.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Category(
    @SerialName("categories")
    val categories: List<Category> = listOf()
) {
    @Serializable
    data class Category(
        @SerialName("active")
        val active: Boolean = false,
        @SerialName("description")
        val description: String? = "",
        @SerialName("_id")
        val id: String = "",
        @SerialName("isWeb")
        val isWeb: Boolean = false,
        @SerialName("link")
        val link: String = "",
        @SerialName("thumb")
        val thumb: Media = Media(),
        @SerialName("title")
        val title: String = ""
    )
}

