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
@Deprecated("建议使用 KotlinLogging", ReplaceWith("io.github.oshai.kotlinlogging.KotlinLogging"))
object BikaLog {
    private const val TAG = "BikaLog"

    // 崩溃可能发生在任意线程（网络/IO 线程），开关与文件引用必须可见
    @Volatile
    private var isLoggingEnabled = false

    @Volatile
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context, enabled: Boolean) {
        isLoggingEnabled = enabled
        // 存 filesDir 而非 cacheDir：系统可能在存储压力大时清理 cache，导致日志丢失
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        logFile = File(logDir, "app.log")
        installCrashHandler()
    }

    fun setLoggingEnabled(enabled: Boolean) {
        isLoggingEnabled = enabled
    }

    fun getLoggingEnabled(): Boolean = isLoggingEnabled

    /**
     * 安装全局未捕获异常处理器：把闪退堆栈同步写入日志文件。
     * 常规日志是异步写入的，进程崩溃时未落盘的数据会丢失，
     * 因此崩溃路径必须同步写，保证"未知条件下闪退"也能被记录。
     */
    private fun installCrashHandler() {
        val current = Thread.getDefaultUncaughtExceptionHandler()
        if (current is CrashLogHandler) return
        Thread.setDefaultUncaughtExceptionHandler(CrashLogHandler(current))
    }

    private class CrashLogHandler(
        private val previous: Thread.UncaughtExceptionHandler?,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            try {
                val stackTrace = Log.getStackTraceString(throwable)
                writeLogSync("FATAL", thread.name, stackTrace)
            } catch (_: Exception) {
                // 崩溃日志写入失败不应影响默认崩溃流程
            } finally {
                previous?.uncaughtException(thread, throwable)
            }
        }
    }

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

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        val fullMessage = if (throwable != null) {
            "$message\n${Log.getStackTraceString(throwable)}"
        } else {
            message
        }
        writeLog("W", tag, fullMessage)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        writeLog("I", tag, message)
    }

    private fun now(): String = synchronized(dateFormat) { dateFormat.format(Date()) }

    private fun writeLog(level: String, tag: String, message: String) {
        if (!isLoggingEnabled) return
        val file = logFile ?: return
        val logLine = "${now()} [$level] $tag: $message\n"
        scope.launch {
            try {
                file.appendText(logLine)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write log to file", e)
            }
        }
    }

    /** 同步写入日志文件（崩溃路径专用，保证进程死亡前落盘）。
     *  崩溃日志无条件写入：用户最需要日志的正是闪退场景，不依赖日志开关。 */
    private fun writeLogSync(level: String, tag: String, message: String) {
        val file = logFile ?: return
        val logLine = "${now()} [$level] $tag: $message\n"
        try {
            file.appendText(logLine)
        } catch (_: Exception) {
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
