package com.shizq.bika.core.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/** 列表卡片本地阅读状态，与作品是否完结独立。 */
@Serializable
enum class ComicReadingStatus {
    HasUpdates,
    Completed,
    Read,
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ComicSummary(
    @SerialName("_id")
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val totalViews: Int = 0,
    val viewsCount: Int = 0,
    val leaderboardCount: Int = 0,
    @JsonNames("likesCount")
    val totalLikes: Int = 0,
    val pagesCount: Int = 0,
    val epsCount: Int = 0,
    val finished: Boolean = false,
    val categories: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    @SerialName("thumb")
    val image: RemoteImage,
    val isFavourited: Boolean = false,
    val readingStatus: ComicReadingStatus? = null,
)

@Serializable
data class RemoteImage(
    val originalName: String = "",
    val path: String = "",
    val fileServer: String = "",
) {
    val originalImageUrl: String
        get() = "${fileServer}/static/${path}"
}
