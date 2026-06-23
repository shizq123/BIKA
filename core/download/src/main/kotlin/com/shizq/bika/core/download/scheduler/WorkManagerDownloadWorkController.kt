package com.shizq.bika.core.download.scheduler

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.shizq.bika.core.download.worker.ChapterDownloadWorker
import com.shizq.bika.core.download.worker.DelegatingWorker
import com.shizq.bika.core.download.worker.DownloadDispatchWorker
import com.shizq.bika.core.download.worker.DownloadWorkSpec
import com.shizq.bika.core.download.worker.delegatedData
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

@Singleton
class WorkManagerDownloadWorkController @Inject constructor(
    @ApplicationContext context: Context,
) : DownloadWorkController {

    private val workManager = WorkManager.getInstance(context)

    override fun enqueueTaskWork(taskId: String, replace: Boolean) {
        val inputData = Data.Builder()
            .putAll(ChapterDownloadWorker::class.delegatedData())
            .putString(DownloadWorkSpec.KEY_TASK_ID, taskId)
            .build()

        val request = OneTimeWorkRequestBuilder<DelegatingWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(DownloadWorkSpec.TAG_CHAPTER_DOWNLOAD)
            .addTag(DownloadWorkSpec.taskTag(taskId))
            .build()

        workManager.enqueueUniqueWork(
            DownloadWorkSpec.uniqueTaskWorkName(taskId),
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override fun cancelTaskWork(taskId: String) {
        workManager.cancelUniqueWork(DownloadWorkSpec.uniqueTaskWorkName(taskId))
    }

    override fun enqueueDispatchWork(
        delayMs: Long,
        replace: Boolean,
    ) {
        val builder = OneTimeWorkRequestBuilder<DelegatingWorker>()
            .setInputData(DownloadDispatchWorker::class.delegatedData())
            .addTag(DownloadWorkSpec.TAG_DOWNLOAD_DISPATCH)

        if (delayMs > 0L) {
            builder.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        }

        workManager.enqueueUniqueWork(
            DownloadWorkSpec.UNIQUE_DISPATCH_WORK,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            builder.build(),
        )
    }

    override suspend fun cancelTaskAndAwaitStopped(
        taskId: String,
        timeoutMs: Long,
    ): Boolean {
        val uniqueName = DownloadWorkSpec.uniqueTaskWorkName(taskId)
        workManager.cancelUniqueWork(uniqueName)

        return withTimeoutOrNull(timeoutMs) {
            while (true) {
                val infos = withContext(Dispatchers.IO) {
                    workManager.getWorkInfosForUniqueWork(uniqueName).get()
                }

                val hasActiveWork = infos.any { info ->
                    info.state == WorkInfo.State.ENQUEUED ||
                            info.state == WorkInfo.State.RUNNING ||
                            info.state == WorkInfo.State.BLOCKED
                }

                if (!hasActiveWork) {
                    return@withTimeoutOrNull true
                }

                delay(200)
            }
        } ?: false
    }
}