package com.shizq.bika.core.model

/**
 * 应用更新的领域模型，由 [com.shizq.bika.core.domain.CheckAppUpdateUseCase] 组装。
 *
 * 不含任何网络层类型（无序列化注解），因此放在 core.model 而非 core.network.model：
 * 它是从 GitHub Releases 响应（[com.shizq.bika.core.network.model.GithubReleaseResponse]）
 * 派生出的纯领域对象，不是接口原样的响应体。
 */
data class AppUpdateRelease(
    val versionName: String,
    val versionCode: Long,
    val changelog: String,
    val downloadUrl: String,
    val forceUpdate: Boolean,
    val apkSize: Long?,
    val apkSha256: String?,
)
