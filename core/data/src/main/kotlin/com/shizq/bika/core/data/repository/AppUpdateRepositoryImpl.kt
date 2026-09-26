package com.shizq.bika.core.data.repository

import android.os.Build
import com.shizq.bika.core.network.GithubDataSource
import com.shizq.bika.core.network.model.GithubAsset
import jakarta.inject.Inject
import java.io.File

class AppUpdateRepositoryImpl @Inject constructor(
    private val githubDataSource: GithubDataSource,
) : AppUpdateRepository {

    override suspend fun checkForUpdate(currentVersionName: String): AppRelease? {
        val release = githubDataSource.getLatestRelease()

        val apkAssets = release.assets.filter { it.name.endsWith(".apk") }
        if (apkAssets.isEmpty()) return null

        val selectedAsset = selectBestApkAsset(apkAssets) ?: return null
        val remoteVersion = extractVersionName(selectedAsset.name) ?: return null

        if (selectedAsset.browserDownloadUrl.isEmpty()) return null
        if (!isNewVersion(currentVersionName, remoteVersion)) return null

        return AppRelease(
            remoteVersion = remoteVersion,
            changelog = release.body,
            downloadUrl = selectedAsset.browserDownloadUrl,
        )
    }

    override suspend fun downloadApk(
        downloadUrl: String,
        destFile: File,
        onProgress: (Float) -> Unit,
    ) {
        if (destFile.exists()) destFile.delete()
        githubDataSource.downloadApk(downloadUrl, destFile, onProgress)
    }

    companion object {
        fun extractVersionName(filename: String): String? {
            val regex = Regex("""_v?([0-9]+(?:\.[0-9]+)+)""")
            return regex.find(filename)?.groupValues?.getOrNull(1)
        }

        fun selectBestApkAsset(
            assets: List<GithubAsset>,
            supportedAbis: Array<String> = runCatching { Build.SUPPORTED_ABIS }.getOrNull() ?: emptyArray(),
        ): GithubAsset? {
            if (assets.isEmpty()) return null
            for (abi in supportedAbis) {
                val matched = assets.firstOrNull { asset ->
                    asset.name.contains(abi, ignoreCase = true) ||
                        (abi == "arm64-v8a" && asset.name.contains("v8a", ignoreCase = true)) ||
                        (abi == "armeabi-v7a" && asset.name.contains("v7a", ignoreCase = true))
                }
                if (matched != null) return matched
            }
            return assets.firstOrNull()
        }

        fun isNewVersion(local: String, remote: String): Boolean {
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
}
