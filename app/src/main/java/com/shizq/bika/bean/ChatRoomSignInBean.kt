package com.shizq.bika.bean

data class ChatRoomSignInBean(
    var token: String="",
    val error: String,
    val message: String,
    val statusCode: Int
)