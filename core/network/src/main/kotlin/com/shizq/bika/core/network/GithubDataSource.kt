package com.shizq.bika.core.network

import com.shizq.bika.core.network.di.GithubClient
import com.shizq.bika.core.network.model.GithubReleaseResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.File

/** 下载链接返回非成功状态时抛出，避免把错误页内容误当 apk 写入本地文件。 */
class ApkDownloadFailedException(val statusCode: Int) :
    Exception("下载失败：HTTP $statusCode")

@Singleton
class GithubDataSource @Inject constructor(
    @GithubClient private val httpClient: HttpClient,
) {
    suspend fun getLatestRelease(): GithubReleaseResponse =
        httpClient
            .get("https://api.github.com/repos/STlxx-lin/BIKA/releases/tags/latest")
            .body()

    suspend fun downloadApk(
        downloadUrl: String,
        destFile: File,
        onProgress: (Float) -> Unit,
    ) {
        httpClient.prepareGet(downloadUrl) {
            onDownload { bytesSentTotal, contentLength ->
                if (contentLength != null && contentLength > 0) {
                    onProgress(bytesSentTotal.toFloat() / contentLength.toFloat())
                }
            }
        }.execute { response ->
            if (!response.status.isSuccess()) {
                throw ApkDownloadFailedException(response.status.value)
            }
            response.bodyAsChannel().copyAndClose(destFile.writeChannel())
        }
    }
}
