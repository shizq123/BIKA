package com.shizq.bika.bean

data class ChatMessage2Bean(
    val `data`: Data,
    val isBlocked: Boolean,
    val type: String,
    val id: String
){
    data class Data(
        val `data`: String,
        val action: String,//"action":"MUTE_USER"
        val message: Message,
        val profile: Profile,
        val reply: Reply
    ){
        data class Message(
            val caption: String,
            val date: String,
            val id: String,
            val medias: List<String>,
            val message: String,
            val referenceId: String,
            val userMentions: List<UserMention>
        )

        data class Profile(
            val avatarUrl: String,
            val birthday: String,
            val characters: List<String>,
            val created_at: String,
            val email: String,
            val exp: Int,
            val gender: String,
            val id: String,
            val level: Int,
            val name: String,
            val role: String,
            val slogan: String,
            val title: String
        )

        data class Reply(
            val id: String,
            val image: String,
            val message: String,
            val name: String,
            val type: String
        )
    }
}
data class UserMention(
    val id: String,
    val name: String
)