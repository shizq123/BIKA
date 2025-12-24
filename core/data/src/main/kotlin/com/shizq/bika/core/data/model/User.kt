package com.shizq.bika.core.data.model

import com.shizq.bika.core.network.model.UserData

class User(
    val id: String,
    val name: String,
    val gender: String,
    val title: String,
    val slogan: String? = null,
    val level: Int,
    val exp: Long,
    val avatar: String,
    val comicsUploaded: Int? = null,
    val character: String? = null,
    val characters: List<String>
)

fun UserData.asExternalModel() = User(
    id = id,
    name = name,
    gender = gender.value,
    title = title,
    slogan = slogan,
    level = level,
    exp = exp,
    avatar = avatar.originalImageUrl,
    comicsUploaded = comicsUploaded,
    character = character,
    characters = characters
)