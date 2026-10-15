package com.shizq.bika.core.common

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class BikaLogStressAdversarialTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testFilesDir: File
    private lateinit var testContext: Context

    private class TestContext(private val filesDirFile: File) : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = filesDirFile
    }

    @Before
    fun setup() {
        testFilesDir = tempFolder.newFolder("files_stress")
        testContext = TestContext(testFilesDir)
    }

    @Test
    fun testHighConcurrencyWritingAndClearing() {
        BikaLog.init(testContext, true)
        val logFile = BikaLog.getLogFile()
        assertNotNull(logFile)

        val writerThreadCount = 8
        val clearThreadCount = 2
        val writesPerThread = 150
        val clearsPerThread = 20

        val executor = Executors.newFixedThreadPool(writerThreadCount + clearThreadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(writerThreadCount + clearThreadCount)
        val errorCount = AtomicInteger(0)

        // 写入工作者
        for (i in 0 until writerThreadCount) {
            val threadId = i
            executor.submit {
                try {
                    startLatch.await(5, TimeUnit.SECONDS)
                    for (j in 0 until writesPerThread) {
                        when (j % 4) {
                            0 -> BikaLog.e("Thread-$threadId", "Error msg $j", RuntimeException("Worker exception $j"))
                            1 -> BikaLog.w("Thread-$threadId", "Warn msg $j")
                            2 -> BikaLog.i("Thread-$threadId", "Info msg $j")
                            else -> BikaLog.d("Thread-$threadId", "Debug msg $j")
                        }
                    }
                } catch (t: Throwable) {
                    errorCount.incrementAndGet()
                    t.printStackTrace()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        // 清空工作者
        for (i in 0 until clearThreadCount) {
            executor.submit {
                try {
                    startLatch.await(5, TimeUnit.SECONDS)
                    for (j in 0 until clearsPerThread) {
                        BikaLog.clearLogs()
                        Thread.sleep(2)
                    }
                } catch (t: Throwable) {
                    errorCount.incrementAndGet()
                    t.printStackTrace()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        val finished = doneLatch.await(20, TimeUnit.SECONDS)
        executor.shutdown()

        assertTrue("并发测试应在规定时限内执行完毕，无死锁", finished)
        assertEquals("多线程并发写入与连续清空过程中严禁产生未捕获异常", 0, errorCount.get())

        // 压力测试后，验证句柄正常，依然能写入新日志
        BikaLog.e("PostStress", "Alive and healthy post stress")
        assertTrue(logFile!!.exists())
        val finalContent = logFile.readText()
        assertTrue("清空与并发写后依然能持久化最新日志", finalContent.contains("Alive and healthy post stress"))
    }

    @Test
    fun testDeletedLogFileAutoRecreation() {
        BikaLog.init(testContext, true)
        val logFile = BikaLog.getLogFile()!!
        assertTrue(logFile.exists())

        // 模拟外部意外或恶意删除 app.log
        assertTrue(logFile.delete())
        assertTrue(!logFile.exists())

        // 触发日志落盘，FileOutputStream(file, true) 应当自愈重新创建该文件
        BikaLog.e("RecoveryTag", "Log after file deleted")

        assertTrue("文件被外部删除后应能自动重新创建", logFile.exists())
        assertTrue(logFile.readText().contains("Log after file deleted"))
    }

    @Test
    fun testDeletedLogsDirectoryGracefulDegradation() {
        BikaLog.init(testContext, true)
        val logFile = BikaLog.getLogFile()!!
        val parentDir = logFile.parentFile!!
        assertTrue(parentDir.exists())

        // 模拟外部极端情况直接删除整个 logs 目录
        parentDir.deleteRecursively()
        assertTrue(!parentDir.exists())

        // 触发落盘和清空操作，必须优雅降级，绝对不崩溃
        try {
            BikaLog.e("DegradationTag", "Writing when directory is gone")
            BikaLog.clearLogs()
        } catch (t: Throwable) {
            throw AssertionError("目录被删除时严禁抛出未捕获异常", t)
        }
    }

    @Test
    fun testExtremeCharactersAndLargeMessage() {
        BikaLog.init(testContext, true)
        val logFile = BikaLog.getLogFile()!!
        BikaLog.clearLogs()

        val largePayload = "X".repeat(50_000)
        val specialChars = "\n\r\t 特殊符号：🔥🦄🚀 \u3000 全角空格 \u0000 空字符 JSON:{\"key\":\"val\"}"
        val compositeException = RuntimeException("Root crash", IllegalArgumentException("Inner cause"))

        BikaLog.e("ExtremeTag", largePayload + specialChars, compositeException)

        assertTrue(logFile.exists())
        val content = logFile.readText()
        assertTrue("能写入大消息", content.length >= 50_000)
        assertTrue("能保留特殊符号", content.contains("🔥🦄🚀"))
        assertTrue("能包含嵌套异常栈", content.contains("Root crash") && content.contains("Inner cause"))
    }

    @Test
    fun testContinuousTruncateWithoutLeak() {
        BikaLog.init(testContext, true)
        val logFile = BikaLog.getLogFile()!!

        for (i in 0 until 50) {
            BikaLog.e("LoopTag", "Iteration $i message")
            assertTrue(logFile.length() > 0)
            BikaLog.clearLogs()
            assertEquals("清空后长度应立即为 0", 0L, logFile.length())
        }

        BikaLog.e("LoopTag", "Final message")
        assertTrue(logFile.readText().contains("Final message"))
    }
}
