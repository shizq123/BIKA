package com.shizq.bika.bean


data class GamesBean(
    val games: Games
) {
    data class Games(
        val docs: List<Docs>,
        val limit: Int,
        val page: Int,
        val pages: Int,
        val total: Int
    ) {
        data class Docs(
            val adult: Boolean,
            val android: Boolean,
            val icon: Icon,
            val _id: String,
            val ios: Boolean,
            val publisher: String,
            val suggest: Boolean,
            val title: String,
            val version: String
        ) {
            data class Icon(
                val fileServer: String,
                val originalName: String,
                val path: String
            )
        }
    }
}








