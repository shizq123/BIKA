package com.shizq.bika.sync.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.tracing.traceAsync
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.sync.initializers.SyncConstraints
import com.shizq.bika.sync.status.SyncSubscriber
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okio.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Syncs the data layer by delegating to the appropriate repository instances with
 * sync functionality.
 */
@HiltWorker
internal class SyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncSubscriber: SyncSubscriber,
    private val api: BikaDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(IO) {
        traceAsync("Sync", 0) {
            syncSubscriber.subscribe()

            try {
                val config = api.getNetworkConfig()

                userPreferencesDataSource.setDns(config.addresses.toSet())
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, "Sync failed with a transient error, retrying.", e)
                Result.retry()
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected error occurred during sync.", e)
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val UNIQUE_WORK_NAME = "DailySyncWorker"

        /**
         * Expedited one time work to sync data on app startup
         */
        fun startUpSyncWork() = OneTimeWorkRequestBuilder<DelegatingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .setInputData(SyncWorker::class.delegatedData())
            .build()

        fun scheduleDailySync(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<DelegatingWorker>(24, TimeUnit.HOURS)
                .setConstraints(SyncConstraints)
                .setInputData(SyncWorker::class.delegatedData())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
    }
}