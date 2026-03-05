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

@Serializable
data class GameDetailsDataa(
    @SerialName("game")
    val details: GameDetails
)

@Serializable
data class GameDetails(
    @SerialName("_id")
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val version: String = "",
    val icon: Image2,
    val publisher: String = "",
    val ios: Boolean = false,
    val iosLinks: List<String> = emptyList(),
    val android: Boolean = false,
    val androidLinks: List<String> = emptyList(),
    val adult: Boolean = false,
    val suggest: Boolean = false,
    val downloadsCount: Int = 0,
    val screenshots: List<Image2> = emptyList(),
    val androidSize: Long = 0,
    val iosSize: Long = 0,
    val updateContent: String = "",
    val videoLink: String = "",
    @SerialName("updated_at")
    val updatedAt: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    val picaLink: Boolean = false,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val commentsCount: Int = 0
)