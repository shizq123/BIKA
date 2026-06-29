package com.shizq.bika.core.domain

import com.shizq.bika.core.data.platform.UpdateFileProvider
import com.shizq.bika.core.data.repository.AppUpdateRepository
import com.shizq.bika.core.network.model.AppUpdateRelease
import java.io.File
import javax.inject.Inject

class DownloadUpdateApkUseCase @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository,
    private val updateFileProvider: UpdateFileProvider,
) {

    suspend operator fun invoke(
        release: AppUpdateRelease,
        onProgress: (Float) -> Unit,
    ): File {
        require(release.downloadUrl.startsWith("https://")) {
            "更新包下载地址必须使用 HTTPS"
        }

        val apkFile = updateFileProvider.getApkFile(
            versionCode = release.versionCode,
        )

        if (apkFile.exists()) {
            apkFile.delete()
        }

        appUpdateRepository.downloadApk(
            downloadUrl = release.downloadUrl,
            destFile = apkFile,
            onProgress = { progress ->
                onProgress(progress.coerceIn(0f, 1f))
            },
        )

        onProgress(1f)

        return apkFile
    }
}