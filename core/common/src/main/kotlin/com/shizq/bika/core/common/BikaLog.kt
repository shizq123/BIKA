package com.shizq.bika.core.common

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BikaLog {
    private const val TAG = "BikaLog"
    private var isLoggingEnabled = false
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context, enabled: Boolean) {
        isLoggingEnabled = enabled
        val logDir = File(context.cacheDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        logFile = File(logDir, "app.log")
    }

    fun setLoggingEnabled(enabled: Boolean) {
        isLoggingEnabled = enabled
    }

    fun getLoggingEnabled(): Boolean = isLoggingEnabled

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeLog("D", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val fullMessage = if (throwable != null) {
            "$message\n${Log.getStackTraceString(throwable)}"
        } else {
            message
        }
        writeLog("E", tag, fullMessage)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        writeLog("W", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        writeLog("I", tag, message)
    }

    private fun writeLog(level: String, tag: String, message: String) {
        if (!isLoggingEnabled) return
        val file = logFile ?: return
        scope.launch {
            try {
                val timestamp = dateFormat.format(Date())
                val logLine = "$timestamp [$level] $tag: $message\n"
                file.appendText(logLine)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write log to file", e)
            }
        }
    }

    fun getLogFile(): File? {
        return logFile
    }

    fun clearLogs() {
        scope.launch {
            try {
                logFile?.let {
                    if (it.exists()) {
                        it.writeText("") // 清空
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear logs", e)
            }
        }
    }
}
