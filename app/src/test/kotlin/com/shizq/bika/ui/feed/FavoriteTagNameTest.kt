package com.shizq.bika.ui.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FavoriteTagNameTest {

    @Test
    fun `名称会去除首尾空白`() {
        assertEquals("标签名称", normalizeFavoriteTagName("  标签名称  "))
    }

    @Test
    fun `纯空白名称无效`() {
        assertNull(normalizeFavoriteTagName("   \n\t"))
    }
}
