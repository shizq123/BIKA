package com.shizq.bika.core.download

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.shizq.bika.core.download.worker.DelegatingWorker
import com.shizq.bika.core.download.worker.DownloadDispatchWorker
import com.shizq.bika.core.download.worker.DownloadWorkSpec
import com.shizq.bika.core.download.worker.delegatedData

object Download {
    fun initialize(context: Context) {
        val request = OneTimeWorkRequestBuilder<DelegatingWorker>()
            .setInputData(DownloadDispatchWorker::class.delegatedData())
            .addTag(DownloadWorkSpec.TAG_DOWNLOAD_DISPATCH)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DownloadWorkSpec.UNIQUE_DISPATCH_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}