package com.shizq.bika.core.download.executor

import android.util.Log
import com.shizq.bika.core.database.model.DownloadErrorCode
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.download.monitor.NetworkMonitor
import com.shizq.bika.core.download.repository.DownloadTaskRepository
import com.shizq.bika.core.download.storage.LocalComicStorage
import com.shizq.bika.core.network.BikaDataSource
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resumeWithException
import kotlin.time.Clock

@Singleton
class DefaultChapterDownloadExecutor @Inject constructor(
    private val network: BikaDataSource,
    private val okHttpClient: OkHttpClient,
    private val storage: LocalComicStorage,
    private val repository: DownloadTaskRepository,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val networkMonitor: NetworkMonitor,
    private val clock: Clock,
) : ChapterDownloadExecutor {

    companion object {
        private const val TAG = "ChapterDownloadExec"

        /** 单章节内页下载并发 */
        private const val PAGE_PARALLELISM = 5

        /** 单页最大重试次数（不含首次） */
        private const val MAX_PAGE_RETRY_COUNT = 2

        /** 进度最短刷新间隔 */
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
    }

    override suspend fun execute(
        task: DownloadTask,
        workerToken: String,
    ): ChapterDownloadResult =
        withContext(Dispatchers.IO) {
            try {
                val constraints = loadConstraints()

                ensureNetworkConstraints(constraints)

                val pageUrls = fetchAllPageUrls(task, constraints)
                if (pageUrls.isEmpty()) {
                    return@withContext ChapterDownloadResult.Failed(
                        errorCode = DownloadErrorCode.NO_IMAGES_FOUND,
                        message = "章节未返回任何可下载图片",
                        recoverable = false,
                    )
                }

                val episodeDir = storage.prepareEpisodeDir(task.comicId, task.episodeOrder)
                repository.updateLocalPath(task.id, episodeDir.absolutePath)

                val totalPages = pageUrls.size
                // 单次扫描目录得出已完成页码集合，避免逐页重复 listFiles() 的 O(N²) I/O。
                val existingPages = storage.findExistingPageNumbers(episodeDir)
                val completedPages = AtomicInteger(
                    existingPages.count { it in 1..totalPages }
                )

                val progressReporter = ProgressReporter(
                    taskId = task.id,
                    workerToken = workerToken,
                    repository = repository,
                    clock = clock,
                )

                // 先同步一次恢复后的初始进度
                progressReporter.report(
                    downloadedPages = completedPages.get(),
                    totalPages = totalPages,
                    force = true,
                )

                val semaphore = Semaphore(PAGE_PARALLELISM)

                coroutineScope {
                    pageUrls.mapIndexed { index, imageUrl ->
                        val pageNumber = index + 1
                        async {
                            semaphore.withPermit {
                                coroutineContext.ensureActive()
                                ensureNetworkConstraints(constraints)

                                if (pageNumber in existingPages) {
                                    return@withPermit
                                }

                                downloadPageWithRetry(
                                    dir = episodeDir,
                                    pageNumber = pageNumber,
                                    imageUrl = imageUrl,
                                    constraints = constraints,
                                )

                                val current = completedPages.incrementAndGet()
                                progressReporter.report(
                                    downloadedPages = current,
                                    totalPages = totalPages,
                                    force = current == totalPages,
                                )
                            }
                        }
                    }.awaitAll()
                }

                progressReporter.report(
                    downloadedPages = totalPages,
                    totalPages = totalPages,
                    force = true,
                )

                ChapterDownloadResult.Success(
                    localPath = episodeDir.absolutePath,
                    totalPages = totalPages,
                )
            } catch (e: CancellationException) {
                Log.i(TAG, "下载被取消: taskId=${task.id}")
                throw e
            } catch (e: WaitingForNetworkException) {
                Log.i(TAG, "等待网络: taskId=${task.id}, reason=${e.message}")
                ChapterDownloadResult.WaitingForNetwork(
                    errorCode = e.errorCode,
                    message = e.message,
                )
            } catch (e: Throwable) {
                Log.e(TAG, "下载失败: taskId=${task.id}", e)
                e.toFailedResult()
            }
        }

    private suspend fun loadConstraints(): DownloadConstraints {
        val userData = userPreferencesDataSource.userData.first()
        return DownloadConstraints(
            wifiOnly = userData.downloadOverWifiOnly,
        )
    }

    private fun ensureNetworkConstraints(constraints: DownloadConstraints) {
        if (!networkMonitor.isConnected()) {
            throw WaitingForNetworkException(
                errorCode = DownloadErrorCode.NETWORK_UNAVAILABLE,
                message = "当前无可用网络，任务等待恢复",
            )
        }

        if (constraints.wifiOnly && !networkMonitor.isWifiConnected()) {
            throw WaitingForNetworkException(
                errorCode = DownloadErrorCode.WIFI_REQUIRED,
                message = "已开启仅 Wi‑Fi 下载，当前网络不满足条件",
            )
        }
    }

    private suspend fun fetchAllPageUrls(
        task: DownloadTask,
        constraints: DownloadConstraints,
    ): List<String> {
        val allPages = mutableListOf<String>()
        var page = 1

        while (true) {
            currentCoroutineContext().ensureActive()
            ensureNetworkConstraints(constraints)

            val response = network.getChapterPages(
                id = task.comicId,
                order = task.episodeOrder,
                page = page,
            )

            val pagination = response.paginationData
            val images = pagination.images
                .mapNotNull { it.media.originalImageUrl.takeIf(String::isNotBlank) }

            allPages.addAll(images)

            val remoteTotalPages = pagination.totalPages
            val shouldStop = images.isEmpty() || remoteTotalPages <= page || remoteTotalPages <= 0
            if (shouldStop) break

            page++
        }

        return allPages
    }

    private suspend fun downloadPageWithRetry(
        dir: File,
        pageNumber: Int,
        imageUrl: String,
        constraints: DownloadConstraints,
    ) {
        var lastError: Throwable? = null

        repeat(MAX_PAGE_RETRY_COUNT + 1) { attempt ->
            currentCoroutineContext().ensureActive()
            ensureNetworkConstraints(constraints)

            try {
                downloadSinglePage(
                    dir = dir,
                    pageNumber = pageNumber,
                    imageUrl = imageUrl,
                )
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastError = e

                val canRetry = attempt < MAX_PAGE_RETRY_COUNT && e.isRetryable()
                if (!canRetry) {
                    throw e
                }

                val backoffMs = 500L * (attempt + 1)
                Log.w(
                    TAG,
                    "页下载失败，准备重试: page=$pageNumber, attempt=${attempt + 1}, delay=${backoffMs}ms",
                    e,
                )
                delay(backoffMs)
            }
        }

        throw lastError ?: IllegalStateException("未知页下载错误")
    }

    private suspend fun downloadSinglePage(
        dir: File,
        pageNumber: Int,
        imageUrl: String,
    ) {
        val request = Request.Builder()
            .url(imageUrl)
            .build()

        executeCallCancellable(request).use { response ->
            if (!response.isSuccessful) {
                throw HttpStatusException(
                    code = response.code,
                    url = imageUrl,
                )
            }

            val body = response.body
            val contentType = body.contentType()?.toString()

            val extension = storage.resolveImageExtension(
                url = imageUrl,
                contentType = contentType,
            )

            val targetFile = File(
                dir,
                storage.buildPageFileName(pageNumber, extension),
            )

            body.source().use { source ->
                storage.writePageAtomically(targetFile, source)
            }
        }
    }

    private suspend fun executeCallCancellable(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = okHttpClient.newCall(request)
            continuation.invokeOnCancellation {
                call.cancel()
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!continuation.isActive) {
                        response.close()
                        return
                    }
                    continuation.resume(response) { _, response, _ -> response.close() }
                }
            })
        }

    private fun Throwable.toFailedResult(): ChapterDownloadResult.Failed {
        return when (this) {
            is HttpStatusException -> {
                ChapterDownloadResult.Failed(
                    errorCode = DownloadErrorCode.HTTP_ERROR,
                    message = "图片请求失败，HTTP $code",
                    recoverable = code >= 500 || code == 429,
                )
            }

            is NoImagesFoundException -> {
                ChapterDownloadResult.Failed(
                    errorCode = DownloadErrorCode.NO_IMAGES_FOUND,
                    message = message,
                    recoverable = false,
                )
            }

            is UnknownHostException,
            is ConnectException,
            is SocketTimeoutException -> {
                ChapterDownloadResult.Failed(
                    errorCode = DownloadErrorCode.NETWORK_UNAVAILABLE,
                    message = localizedMessage ?: "网络连接失败",
                    recoverable = true,
                )
            }

            is IOException -> {
                ChapterDownloadResult.Failed(
                    errorCode = DownloadErrorCode.IO_ERROR,
                    message = localizedMessage ?: "本地文件写入失败",
                    recoverable = true,
                )
            }

            else -> {
                ChapterDownloadResult.Failed(
                    errorCode = DownloadErrorCode.UNKNOWN,
                    message = localizedMessage ?: "未知下载错误",
                    recoverable = false,
                )
            }
        }
    }

    private fun Throwable.isRetryable(): Boolean {
        return when (this) {
            is HttpStatusException -> code >= 500 || code == 429
            is UnknownHostException,
            is ConnectException,
            is SocketTimeoutException,
            is IOException -> true

            else -> false
        }
    }

    private data class DownloadConstraints(
        val wifiOnly: Boolean,
    )

    private class WaitingForNetworkException(
        val errorCode: DownloadErrorCode,
        override val message: String,
    ) : IllegalStateException(message)

    private class NoImagesFoundException(
        override val message: String,
    ) : IllegalStateException(message)

    private class HttpStatusException(
        val code: Int,
        val url: String,
    ) : IOException("HTTP $code for $url")

    private class ProgressReporter(
        private val taskId: String,
        private val workerToken: String,
        private val repository: DownloadTaskRepository,
        private val clock: Clock,
    ) {
        private val mutex = Mutex()

        private var lastEmitAtMs: Long = 0L
        private var lastDownloadedPages: Int = -1
        private var lastProgress: Int = -1

        suspend fun report(
            downloadedPages: Int,
            totalPages: Int,
            force: Boolean,
        ) {
            mutex.withLock {
                val nowMs = clock.now().toEpochMilliseconds()

                val progress = if (totalPages <= 0) {
                    0
                } else {
                    ((downloadedPages * 100.0) / totalPages)
                        .toInt()
                        .coerceIn(0, 100)
                }

                val shouldEmit = force || (
                        lastDownloadedPages != downloadedPages &&
                                (nowMs - lastEmitAtMs >= PROGRESS_UPDATE_INTERVAL_MS || progress != lastProgress)
                        )

                if (!shouldEmit) return

                repository.updateProgressOwned(
                    taskId = taskId,
                    workerToken = workerToken,
                    downloadedPages = downloadedPages,
                    totalPages = totalPages,
                )

                lastEmitAtMs = nowMs
                lastDownloadedPages = downloadedPages
                lastProgress = progress
            }
        }
    }
}