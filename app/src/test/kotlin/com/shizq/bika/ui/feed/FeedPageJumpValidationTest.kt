package com.shizq.bika.ui.feed

import kotlin.test.Test
import kotlin.test.assertEquals

class FeedPageJumpValidationTest {

    @Test
    fun `合法页码返回去除空格后的数字`() {
        assertEquals(
            PageJumpValidationResult(page = 3),
            validatePageJump(" 3 ", totalPages = 10),
        )
    }

    @Test
    fun `空白和非数字输入返回数字错误`() {
        assertEquals(
            PageJumpValidationError.InvalidNumber,
            validatePageJump("", totalPages = 10).error,
        )
        assertEquals(
            PageJumpValidationError.InvalidNumber,
            validatePageJump("abc", totalPages = 10).error,
        )
    }

    @Test
    fun `页码必须位于有效范围内`() {
        assertEquals(
            PageJumpValidationError.BelowMinimum,
            validatePageJump("0", totalPages = 10).error,
        )
        assertEquals(
            PageJumpValidationError.AboveMaximum,
            validatePageJump("11", totalPages = 10).error,
        )
    }

    @Test
    fun `异常总页数按至少一页处理`() {
        assertEquals(
            PageJumpValidationResult(page = 1),
            validatePageJump("1", totalPages = 0),
        )
        assertEquals(
            PageJumpValidationError.AboveMaximum,
            validatePageJump("2", totalPages = 0).error,
        )
    }
}
