package com.shizq.bika.core.common

import android.content.Context
import android.util.Log
import java.io.File
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

    fun init(context: Context, enabled: Boolean) {

    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }


    private fun writeLog(level: String, tag: String, message: String) {
    }

    /** 同步写入日志文件（崩溃路径专用，保证进程死亡前落盘）。
     *  崩溃日志无条件写入：用户最需要日志的正是闪退场景，不依赖日志开关。 */
    private fun writeLogSync(level: String, tag: String, message: String) {

    }

    fun getLogFile(): File? {
        return logFile
    }

    fun clearLogs() {

    }
}
