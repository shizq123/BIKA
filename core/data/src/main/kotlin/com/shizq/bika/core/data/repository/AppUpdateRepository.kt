package com.shizq.bika.core.data.repository

/**
 * 版本更新信息，从 GitHub Releases API 解析而来。
 */
data class AppRelease(
    val remoteVersion: String,
    val changelog: String,
    val downloadUrl: String,
)

interface AppUpdateRepository {
    /**
     * 检查是否有新版本。
     * @param currentVersionName 当前 app 版本号，如 "1.0.8"
     * @return 有新版本时返回 [AppRelease]，否则返回 null
     */
    suspend fun checkForUpdate(currentVersionName: String): AppRelease?
}
