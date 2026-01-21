package com.shizq.bika.core.network.model

import com.shizq.bika.core.model.Image2
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameData(
    val games: PageData<Game>
)

/**
 * @property adult 是否含有成人内容
 */
@Serializable
data class Game(
    @SerialName("_id")
    val id: String = "",
    val title: String = "",
    val version: String = "",
    val publisher: String = "",
    val suggest: Boolean = false,
    val adult: Boolean = false,
    val android: Boolean = false,
    val ios: Boolean = false,
    val picaLink: Boolean = false,
    val icon: Image2,
    val androidLinks: List<String> = emptyList(),
    val iosLinks: List<String> = emptyList()
)