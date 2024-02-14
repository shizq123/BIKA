package com.shizq.bika.bean

data class ChatRoomListBean(
    val error: String,
    val message: String,
    val statusCode: Int,
    val rooms: List<Room>?
) {
    data class Room(
        val allowedCharacters: List<Any>,
        val description: String,
        val icon: String,
        val id: String,
        val isAvailable: Boolean,
        val isPublic: Boolean,
        val minLevel: Int,
        val minRegisterDays: Int,
        val title: String
    )
}