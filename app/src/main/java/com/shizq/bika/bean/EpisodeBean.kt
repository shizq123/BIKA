package com.shizq.bika.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames


data class EpisodeBean(
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

@Serializable
data class Catalogue(
    @SerialName("eps")
    val eps: Eps = Eps()
) {
    @Serializable
    data class Eps(
        @SerialName("docs")
        val docs: List<Doc> = listOf(),
        @SerialName("limit")
        val limit: Int = 0,
        @SerialName("page")
        val page: Int = 0,
        @SerialName("pages")
        val pages: Int = 0,
        @SerialName("total")
        val total: Int = 0
    ) {
        @Serializable
        data class Doc(
            @JsonNames("_id", "id")
            val id: String = "",
            @SerialName("order")
            val order: Int = 0,
            @SerialName("title")
            val title: String = "",
            @SerialName("updated_at")
            val updatedAt: String = ""
        )
    }
}