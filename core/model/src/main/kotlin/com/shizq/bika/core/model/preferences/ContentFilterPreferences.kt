package com.shizq.bika.core.model.preferences

import com.shizq.bika.core.model.FavoriteTag
import kotlinx.serialization.Serializable

@Serializable
data class ContentFilterPreferences(
    val excludeTopicsGlobal: Boolean = false,
    val globalExcludedTopics: List<String> = emptyList(),
    val favoriteTags: List<FavoriteTag> = emptyList(),
    val blockedTags: Set<String> = emptySet(),
)

@JvmInline
@Serializable
value class TopicName(val value: String)

@JvmInline
@Serializable
value class TagName(val value: String)