package com.shizq.bika.sync.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.OutOfQuotaPolicy
import com.shizq.bika.core.data.repository.DownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 后台执行漫画章节图片下载的 Worker。
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadRepository: DownloadRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val comicId = inputData.getString(KEY_COMIC_ID) ?: return@withContext Result.failure()
        val comicTitle = inputData.getString(KEY_COMIC_TITLE) ?: return@withContext Result.failure()
        val coverUrl = inputData.getString(KEY_COVER_URL) ?: return@withContext Result.failure()
        val episodeId = inputData.getString(KEY_EPISODE_ID) ?: return@withContext Result.failure()
        val episodeTitle = inputData.getString(KEY_EPISODE_TITLE) ?: return@withContext Result.failure()
        val episodeOrder = inputData.getInt(KEY_EPISODE_ORDER, -1)

        if (episodeOrder == -1) return@withContext Result.failure()

        Log.i(TAG, "Starting download for $comicTitle - $episodeTitle")

        try {
            downloadRepository.downloadEpisode(
                comicId = comicId,
                comicTitle = comicTitle,
                coverUrl = coverUrl,
                episodeId = episodeId,
                episodeTitle = episodeTitle,
                episodeOrder = episodeOrder
            )
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download worker failed for $comicTitle - $episodeTitle", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "DownloadWorker"
        
        const val KEY_COMIC_ID = "comic_id"
        const val KEY_COMIC_TITLE = "comic_title"
        const val KEY_COVER_URL = "cover_url"
        const val KEY_EPISODE_ID = "episode_id"
        const val KEY_EPISODE_TITLE = "episode_title"
        const val KEY_EPISODE_ORDER = "episode_order"

        /** 启动单次下载任务 */
        fun startDownload(
            context: Context,
            comicId: String,
            comicTitle: String,
            coverUrl: String,
            episodeId: String,
            episodeTitle: String,
            episodeOrder: Int
        ) {
            val inputData = Data.Builder()
                .putString(KEY_COMIC_ID, comicId)
                .putString(KEY_COMIC_TITLE, comicTitle)
                .putString(KEY_COVER_URL, coverUrl)
                .putString(KEY_EPISODE_ID, episodeId)
                .putString(KEY_EPISODE_TITLE, episodeTitle)
                .putInt(KEY_EPISODE_ORDER, episodeOrder)
                .putString("RouterWorkerDelegateClassName", DownloadWorker::class.qualifiedName)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<DelegatingWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
