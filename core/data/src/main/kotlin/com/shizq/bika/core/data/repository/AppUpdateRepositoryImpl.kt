package com.shizq.bika.core.data.repository

import com.shizq.bika.core.network.GithubDataSource
import jakarta.inject.Inject
import java.io.File

class AppUpdateRepositoryImpl @Inject constructor(
    private val githubDataSource: GithubDataSource,
) : AppUpdateRepository {

    /**
     * @return 有新版本时返回 [AppRelease]；确认无新版本（未发布 apk 资产、版本号未变化等）
     *   时返回 null；检查本身失败（网络异常、响应解析失败等）会抛出异常，而不是
     *   静默退化为 null。
     *
     * 此前网络异常也被 catch 成 null，导致"无网络"和"已是最新版本"在调用方
     * （[com.shizq.bika.core.domain.CheckAppUpdateUseCase]）眼里完全一样，
     * UpdateStateMachine.checkUpdate 里已经写好的 try/catch（区分失败态 Error
     * 与正常态 NoUpdate）因此永远收不到异常。这里只保留 null 表达"确实没有更新"
     * 这一种语义，真正的失败原样抛出去，交给上层已有的错误处理路径。
     */
    override suspend fun checkForUpdate(currentVersionName: String): AppRelease? {
        val release = githubDataSource.getLatestRelease()

        val apkAsset = release.assets.firstOrNull { asset ->
            asset.name.endsWith(".apk") && asset.name.contains("_v")
        } ?: return null

        val remoteVersion = apkAsset.name
            .substringAfter("_v")
            .substringBefore(".apk")

        if (remoteVersion.isEmpty() || apkAsset.browserDownloadUrl.isEmpty()) return null
        if (!isNewVersion(currentVersionName, remoteVersion)) return null

        return AppRelease(
            remoteVersion = remoteVersion,
            changelog = release.body,
            downloadUrl = apkAsset.browserDownloadUrl,
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
