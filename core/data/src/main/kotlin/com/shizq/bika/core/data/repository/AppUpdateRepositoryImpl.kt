package com.shizq.bika.core.data.repository

import android.util.Log
import com.shizq.bika.core.network.GithubDataSource
import jakarta.inject.Inject
import java.io.File

private const val TAG = "AppUpdateRepository"

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
            Log.e(TAG, "检测版本更新失败", e)
            null
        }
    }

    override suspend fun downloadApk(
        downloadUrl: String,
        destFile: File,
        onProgress: (Float) -> Unit,
    ) {
        if (destFile.exists()) destFile.delete()
        githubDataSource.downloadApk(downloadUrl, destFile, onProgress)
    }

    internal fun isNewVersion(local: String, remote: String): Boolean {
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(localParts.size, remoteParts.size)
        repeat(len) { i ->
            val l = localParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (l > r) return false
        }
        return false
    }
}
