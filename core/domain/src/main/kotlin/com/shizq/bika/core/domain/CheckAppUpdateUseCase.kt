package com.shizq.bika.core.domain

import com.shizq.bika.core.data.platform.AppVersionProvider
import com.shizq.bika.core.data.repository.AppUpdateRepository
import com.shizq.bika.core.network.model.AppUpdateRelease
import javax.inject.Inject

class CheckAppUpdateUseCase @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository,
    private val appVersionProvider: AppVersionProvider,
) {

    suspend operator fun invoke(): AppUpdateRelease? {
        val release =
            appUpdateRepository.checkForUpdate(appVersionProvider.versionName) ?: return null

        return AppUpdateRelease(
            versionName = release.remoteVersion,
            versionCode = parseVersionCodeFallback(release.remoteVersion),
            changelog = release.changelog,
            downloadUrl = release.downloadUrl,
            forceUpdate = false,
            apkSize = null,
            apkSha256 = null,
        )
    }

    private fun parseVersionCodeFallback(versionName: String): Long {
        return versionName
            .split(".")
            .mapNotNull { it.toLongOrNull() }
            .fold(0L) { acc, part -> acc * 100 + part }
    }
}