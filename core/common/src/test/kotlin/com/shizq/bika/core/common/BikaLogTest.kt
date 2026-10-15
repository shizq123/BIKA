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

class BikaLogTest {

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
        testFilesDir = tempFolder.newFolder("files")
        testContext = TestContext(testFilesDir)
    }

    @Test
    fun testInit_createsLogFileAndDirectory() {
        BikaLog.init(testContext, true)
        val logFile = BikaLog.getLogFile()

        assertNotNull(logFile)
        assertTrue(logFile!!.exists())
        assertEquals("app.log", logFile.name)
        assertEquals(File(testFilesDir, "logs").absolutePath, logFile.parentFile?.absolutePath)
    }

    @Test
    fun testWriteLogAndClearLogs() {
        BikaLog.init(testContext, true)
        val logFile = BikaLog.getLogFile()!!

        BikaLog.e("TestTag", "Error message occurred")
        assertTrue(logFile.readText().contains("Error message occurred"))

        // 测试创建滚动归档文件
        val archiveFile = File(logFile.parentFile, "app-2026-10-14.log")
        archiveFile.writeText("Old archive log")
        assertTrue(archiveFile.exists())

        // 执行清理截断
        BikaLog.clearLogs()

        assertEquals(0L, logFile.length())
        assertTrue(!archiveFile.exists())
    }

    @Test
    fun testDisabledLoggingDoesNotWriteDebug() {
        BikaLog.init(testContext, false)
        val logFile = BikaLog.getLogFile()!!
        BikaLog.clearLogs()

        BikaLog.d("TestTag", "Debug message should be ignored")
        assertEquals(0L, logFile.length())

        BikaLog.init(testContext, true)
        BikaLog.d("TestTag", "Debug message should be logged")
        assertTrue(logFile.readText().contains("Debug message should be logged"))
    }
}
