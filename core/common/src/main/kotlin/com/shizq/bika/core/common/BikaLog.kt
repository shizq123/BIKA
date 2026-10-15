package com.shizq.bika.core.common

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃日志与日志文件管理（clearLogs/getLogFile 等）。
 *
 * 常规业务日志已迁移到 KotlinLogging（见各模块 logger 定义），
 * 此处保留的是 KotlinLogging 没有等价能力的部分：
 * 进程崩溃同步落盘、日志文件清理与定向导出。
 */
object BikaLog {

    @Volatile
    private var logFile: File? = null

    @Volatile
    private var isLoggingEnabled: Boolean = false

    private val lock = Any()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context, enabled: Boolean) {
        synchronized(lock) {
            isLoggingEnabled = enabled
            val logsDir = File(context.applicationContext.filesDir, "logs")
            if (!logsDir.exists()) {
                logsDir.mkdirs()
            }
            val file = File(logsDir, "app.log")
            if (!file.exists()) {
                try {
                    file.createNewFile()
                } catch (e: Exception) {
                    try {
                        Log.e("BikaLog", "Failed to create log file: ${file.absolutePath}", e)
                    } catch (_: Exception) {}
                }
            }
            logFile = file
        }
    }

    fun d(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: Exception) {}
        if (isLoggingEnabled) {
            writeLog("DEBUG", tag, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, message, throwable)
        } catch (_: Exception) {}
        writeLogSync("ERROR", tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        try {
            Log.w(tag, message, throwable)
        } catch (_: Exception) {}
        if (isLoggingEnabled) {
            writeLog("WARN", tag, message, throwable)
        }
    }

    fun i(tag: String, message: String) {
        try {
            Log.i(tag, message)
        } catch (_: Exception) {}
        if (isLoggingEnabled) {
            writeLog("INFO", tag, message)
        }
    }

    private fun writeLog(level: String, tag: String, message: String, throwable: Throwable? = null) {
        writeLogSync(level, tag, message, throwable)
    }

    /**
     * 同步写入日志文件（崩溃路径专用，保证进程死亡前落盘）。
     * 崩溃日志无条件写入：用户最需要日志的正是闪退场景，不依赖日志开关。
     */
    private fun writeLogSync(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val targetFile = logFile ?: return
        synchronized(lock) {
            try {
                val timeStr = dateFormat.format(Date())
                val logLine = buildString {
                    append(timeStr).append(" [").append(level).append("] [").append(tag).append("]: ").append(message).append("\n")
                    if (throwable != null) {
                        val stack = try {
                            Log.getStackTraceString(throwable)
                        } catch (_: Exception) {
                            ""
                        }
                        if (stack.isNotBlank()) {
                            append(stack).append("\n")
                        } else {
                            append(throwable.stackTraceToString()).append("\n")
                        }
                    }
                }
                targetFile.appendText(logLine)
            } catch (e: Exception) {
                try {
                    Log.e("BikaLog", "Failed to write log", e)
                } catch (_: Exception) {}
            }
        }
    }

    fun getLogFile(): File? {
        return logFile
    }

    fun clearLogs() {
        synchronized(lock) {
            try {
                val currentFile = logFile
                if (currentFile != null && currentFile.exists()) {
                    FileOutputStream(currentFile, false).close()
                }
                currentFile?.parentFile?.listFiles()?.forEach { file ->
                    if (file != currentFile && file.name.startsWith("app") && file.name.endsWith(".log")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                try {
                    Log.e("BikaLog", "Failed to clear logs", e)
                } catch (_: Exception) {}
            }
        }
    }
}
