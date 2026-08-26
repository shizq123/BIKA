package com.shizq.bika

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Process
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy.Builder
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.shizq.bika.core.download.Download
import com.shizq.bika.core.logging.LoggingConfigurator
import com.shizq.bika.sync.initializers.Sync
import com.shizq.bika.util.ProfileVerifierLogger
import dagger.hilt.android.HiltAndroidApp
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import java.io.File

@HiltAndroidApp
class BikaApplication : Application(), SingletonImageLoader.Factory {
    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var profileVerifierLogger: ProfileVerifierLogger

    private val logger = KotlinLogging.logger("BikaApplication")

    override fun onCreate() {
        super.onCreate()
        initializeLogging()
        setStrictModePolicy()
        Sync.initialize(this)
        Download.initialize(this)
        profileVerifierLogger()
        setupGlobalExceptionHandler()
        logger.info { "Application initialized successfully" }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader

    /**
     * Return true if the application is debuggable.
     */
    private fun isDebuggable(): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * Set a thread policy that detects all potential problems on the main thread, such as network
     * and disk access.
     *
     * If a problem is found, the offending call will be logged and the application will be killed.
     */
    private fun setStrictModePolicy() {
        if (!isDebuggable()) return
        StrictMode.setThreadPolicy(
            Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
    }

    private fun initializeLogging() {
        try {
            val logsDir = getLogsDir()
            LoggingConfigurator.configureLogging(logsDir)
            logger.info { "Logging initialized at: ${logsDir.absolutePath}" }
        } catch (e: Exception) {
            Log.e("BikaApplication", "Failed to initialize logging", e)
        }
    }

    private fun getLogsDir(): File {
        val logsDir = applicationContext.filesDir.resolve("logs")
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            Log.w("BikaApplication", "Failed to create logs directory: ${logsDir.absolutePath}")
        }
        return logsDir
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                logger.error(throwable) {
                    "FATAL EXCEPTION on thread: ${thread.name}"
                }
            } catch (e: Exception) {
                Log.e("BikaApplication", "Failed to log uncaught exception", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable) ?: run {
                    Log.e(
                        "BikaApplication",
                        "No default exception handler, killing process",
                        throwable
                    )
                    Process.killProcess(Process.myPid())
                }
            }
        }
    }
}
