package com.shizq.bika.core.data.repository

import android.util.Log
import com.shizq.bika.core.network.GithubDataSource
import jakarta.inject.Inject

class AppUpdateRepositoryImpl @Inject constructor(
    private val githubDataSource: GithubDataSource,
) : AppUpdateRepository {

    override suspend fun checkForUpdate(currentVersionName: String): AppRelease? {
        return try {
            val release = githubDataSource.getLatestRelease()

            val apkAsset = release.assets.firstOrNull { asset ->
                asset.name.endsWith(".apk") && asset.name.contains("_v")
            } ?: return null

            val remoteVersion = apkAsset.name
                .substringAfter("_v")
                .substringBefore(".apk")

            if (remoteVersion.isEmpty() || apkAsset.browserDownloadUrl.isEmpty()) return null
            if (!isNewVersion(currentVersionName, remoteVersion)) return null

            AppRelease(
                remoteVersion = remoteVersion,
                changelog = release.body,
                downloadUrl = apkAsset.browserDownloadUrl,
            )
        } catch (e: Exception) {
            Log.e("AppUpdateRepository", "检测版本更新失败", e)
            null
        }
    }

    private fun isNewVersion(local: String, remote: String): Boolean {
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val size = maxOf(localParts.size, remoteParts.size)
        for (i in 0 until size) {
            val lVal = localParts.getOrNull(i) ?: 0
            val rVal = remoteParts.getOrNull(i) ?: 0
            if (rVal > lVal) return true
            if (lVal > rVal) return false
        }
        return false
    }
}
