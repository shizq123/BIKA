package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationsData(
    val notifications: PageData<NotificationDoc>
)

@Serializable
data class NotificationDoc(
    @SerialName("_id")
    val id: String,
    val content: String,
    val cover: Media? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("_redirectId")
    val redirectId: String? = null,
    val redirectType: String? = null,
    @SerialName("_sender")
    val sender: NotificationSender? = null,
    val system: Boolean = false,
    val title: String,
    @SerialName("_user")
    val user: String? = null
)

@Serializable
data class NotificationSender(
    @SerialName("_id")
    val id: String,
    val avatar: Media? = null,
    val name: String = "",
    val gender: String = "",
    val level: Int = 0,
    val role: String = "",
    val title: String = "",
    val slogan: String = "",
    val character: String? = null
)
