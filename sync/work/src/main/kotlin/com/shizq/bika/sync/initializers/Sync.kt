package com.shizq.bika.sync.initializers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.shizq.bika.sync.workers.SyncWorker

object Sync {
    fun initialize(context: Context) {
        WorkManager.getInstance(context).apply {
            enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                SyncWorker.scheduleDailySync(),
            )
        }
    }
}

// This name should not be changed otherwise the app may have concurrent sync requests running
internal const val SYNC_WORK_NAME = "SyncWorkName"