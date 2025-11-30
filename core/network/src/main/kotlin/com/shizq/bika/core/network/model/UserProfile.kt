package com.shizq.bika.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileData(
    val user: UserProfile
)

@Serializable
data class UserProfile(
    @SerialName("_id")
    val id: String,

    val name: String,         // 昵称
    val email: String,        // 邮箱
    val gender: String,       // "m" or "f"
    val title: String,        // 头衔: "萌新"
    val level: Int,           // 等级
    val exp: Int,             // 经验值
    val verified: Boolean,    // 是否认证
    val isPunched: Boolean,   // 是否打卡
    val slogan: String = "",       // 个性签名


    @SerialName("created_at")
    val createdAt: String,    // 创建时间 ISO 格式

    val birthday: String,     // 生日

    val characters: List<String> = emptyList(),

    val character: String? = null, //这是显示的挂件/立绘URL
    val avatar: Media
) {
    val imageUrl: String
        get() = "https://s3.picacomic.com/static/static/${avatar.path}"
    val imageUrl2: String
        get() = "${avatar.fileServer}/static/${avatar.path}"
}


