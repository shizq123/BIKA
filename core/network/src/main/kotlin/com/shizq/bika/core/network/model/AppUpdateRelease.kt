package com.shizq.bika.core.network.model

data class AppUpdateRelease(
    val versionName: String,
    val versionCode: Long,
    val changelog: String,
    val downloadUrl: String,
    val forceUpdate: Boolean,
    val apkSize: Long?,
    val apkSha256: String?,
)