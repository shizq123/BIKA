package com.shizq.bika.core.model.preferences

import kotlinx.serialization.Serializable

/** 本地留存的用户资料快照，供无网络时回退展示；可能过时。 */
@Serializable
data class UserProfileSnapshot(
    val name: String = "",
    val avatarUrl: String = "",
    val level: Int = 0,
    val exp: Int = 0,
    val title: String = "",
    val gender: String = "",
    val slogan: String = "",
    val honorBadges: List<String> = emptyList(),
    /**
     * 是否已完成从旧版 `user_preferences` 扁平结构迁移过来的一次性搬迁。
     *
     * 不用 [name] 是否为空来判断"是否需要迁移"：登出时 [name] 也会被清空
     * （见 `UserProfileSnapshotDataSource.clear`），若继续用它当哨兵，
     * 登出后下次冷启动会被误判为"从未迁移"，一旦上一次 cleanUp 因 IO 异常
     * 未清空旧文件，就会把上一个账号的资料快照从旧存储里恢复回来。
     * 该字段带默认值 false，对已存在的旧缓存文件（无此字段）解码时天然透明。
     */
    val legacyMigrationDone: Boolean = false,
)
