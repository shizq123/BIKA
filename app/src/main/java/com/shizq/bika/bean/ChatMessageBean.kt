package com.shizq.bika.bean


data class ChatMessageBean(
    val at: String,
    val audio: String,
    val avatar: String,
    val block_user_id: String,
    val character: String,
    val event_colors: List<String>,
    val gender: String,
    val image: String,
    val level: Int,
    val message: String,
    val name: String,
    val platform: String,
    val reply: String,
    val reply_name: String,
    val title: String,
    val type: Any,
    val unique_id: String,
    val user_id: String,
    val verified: Boolean
)