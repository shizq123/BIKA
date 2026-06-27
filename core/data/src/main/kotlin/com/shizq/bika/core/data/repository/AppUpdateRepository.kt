package com.shizq.bika.core.data.repository

import java.io.File

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

    /**
     * 下载 APK 到 [destFile]，通过 [onProgress] 回调报告进度（0.0–1.0）。
     * @param downloadUrl APK 的下载地址
     * @param destFile 下载目标文件
     * @param onProgress 进度回调，取值范围 [0f, 1f]
     */
    suspend fun downloadApk(
        downloadUrl: String,
        destFile: File,
        onProgress: (Float) -> Unit,
    )
}
