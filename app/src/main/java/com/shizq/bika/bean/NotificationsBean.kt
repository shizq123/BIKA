package com.shizq.bika.bean

//通知
data class NotificationsBean(
    val notifications: Notifications
) {
    data class Notifications(
        val docs: List<Doc>,
        val limit: Int,
        val page: Int,
        val pages: Int,
        val total: Int
    ){
        data class Doc(
            val content: String,
            val cover: Cover,
            val created_at: String,
            val _id: String,
            val id: String,
            val _redirectId: String,
            val redirectType: String,
            val _sender: Sender,
            val system: Boolean,
            val title: String,
            val updated_at: String,
            val _user: String
        ){
            data class Cover(
                val fileServer: String,
                val originalName: String,
                val path: String
            )
            data class Sender(
                val character: String,
                val characters: List<Any>,
                val exp: Int,
                val gender: String,
                val _id: String,
                val level: Int,
                val name: String,
                val role: String,
                val slogan: String,
                val title: String,
                val avatar: Avatar,
                val verified: Boolean
            ){
                data class Avatar(
                    val fileServer: String,
                    val originalName: String,
                    val path: String
                )
            }
        }
    }
}