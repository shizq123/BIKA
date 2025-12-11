package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("_id")
    val id: String,

    @SerialName("name")
    val name: String,

    @SerialName("gender")
    val gender: String,

    @SerialName("title")
    val title: String,

    @SerialName("slogan")
    val slogan: String? = null,

    @SerialName("level")
    val level: Int,

    @SerialName("exp")
    val exp: Long,

    @SerialName("avatar")
    val avatar: Media,

    @SerialName("comicsUploaded")
    val comicsUploaded: Int,

    @SerialName("role")
    val role: String,

    @SerialName("verified")
    val verified: Boolean,

    @SerialName("character")
    val character: String? = null,

    @SerialName("characters")
    val characters: List<String>
)