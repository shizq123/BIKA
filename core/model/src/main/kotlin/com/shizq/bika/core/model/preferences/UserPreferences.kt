package com.shizq.bika.core.model.preferences

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val reader: ReaderPreferences = ReaderPreferences(),
    val theme: ThemePreferences = ThemePreferences(),
    val network: NetworkPreferences = NetworkPreferences(),
    val download: DownloadPreferences = DownloadPreferences(),
    val filter: ContentFilterPreferences = ContentFilterPreferences(),
    val app: AppPreferences = AppPreferences(),
    val dashboard: DashboardPreferences = DashboardPreferences(),
    /**
     * 仅供迁移读取的历史字段，资料快照已独立到 `user_profile_cache`。
     *
     * 不要在新代码里读写它：这里是用户设置，而资料快照是服务端派生的可弃缓存，
     * 每次刷新资料都重写整个偏好文件既是写放大，也让登出清缓存和清设置纠缠在一起。
     * 保留字段是为了让 UserProfileCacheMigration 能读到旧值——
     * DataStoreJson 开了 ignoreUnknownKeys，删掉字段旧数据会被静默丢弃。
     * 迁移完成后该字段会被清空，可在一个版本之后移除。
     */
    val profile: UserProfileSnapshot = UserProfileSnapshot(),
)