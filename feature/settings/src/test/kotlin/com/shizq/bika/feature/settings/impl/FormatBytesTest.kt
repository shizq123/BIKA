package com.shizq.bika.feature.settings.impl

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatBytesTest {

    @Test
    fun `小于1024字节时以B为单位`() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("1 B", formatBytes(1))
        assertEquals("1023 B", formatBytes(1023))
    }

    @Test
    fun `达到1024字节时进位为KB`() {
        assertEquals("1 KB", formatBytes(1024))
        assertEquals("1.5 KB", formatBytes(1536))
    }

    @Test
    fun `达到1024KB时进位为MB`() {
        assertEquals("1 MB", formatBytes(1024L * 1024))
        assertEquals("2.5 MB", formatBytes((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun `达到1024MB时进位为GB`() {
        assertEquals("1 GB", formatBytes(1024L * 1024 * 1024))
        assertEquals("3.14 GB", formatBytes((3.14 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `小数保留最多两位且不补零`() {
        // 1024 * 1.1 = 1126.4 -> 1.1 KB (不应显示为 1.10)
        assertEquals("1.1 KB", formatBytes(1126))
    }
}
