package com.shizq.bika.core.model.preferences

import com.shizq.bika.core.model.FavoriteTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContentFilterPreferences(
    @SerialName("excludeTopicsGlobal")
    val globalTopicBlockEnabled: Boolean = false,
    @SerialName("globalExcludedTopics")
    val globalBlockedTopics: List<String> = emptyList(),
    val favoriteTags: List<FavoriteTag> = emptyList(),
    val blockedTags: Set<String> = emptySet(),
)
