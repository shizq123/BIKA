package com.shizq.bika.core.common

import android.content.Context
import android.util.Log
import java.io.File

@Deprecated("建议使用 KotlinLogging", ReplaceWith("io.github.oshai.kotlinlogging.KotlinLogging"))
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
