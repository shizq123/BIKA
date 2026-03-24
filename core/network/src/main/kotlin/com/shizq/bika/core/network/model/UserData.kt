package com.shizq.bika.core.network.model

import com.shizq.bika.core.network.utils.Gender
import com.shizq.bika.core.network.utils.GenderSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    @SerialName("_id")
    val id: String = "",

    @SerialName("name")
    val name: String = "",

    @SerialName("gender")
    @Serializable(with = GenderSerializer::class)
    val gender: Gender = Gender.BOT,

    @SerialName("title")
    val title: String = "",

    @SerialName("slogan")
    val slogan: String? = null,

    @SerialName("level")
    val level: Int = 0,

    @SerialName("exp")
    val exp: Long = 0,

    @SerialName("avatar")
    val avatar: Media? = null,

    @SerialName("comicsUploaded")
    val comicsUploaded: Int? = null,

    @SerialName("role")
    val role: String = "",

    @SerialName("verified")
    val verified: Boolean = false,

    @SerialName("character")
    val character: String? = null,

    @SerialName("characters")
    val characters: List<String> = emptyList()
)