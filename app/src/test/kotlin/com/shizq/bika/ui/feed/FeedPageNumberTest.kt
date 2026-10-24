package com.shizq.bika.ui.feed

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeedPageNumberTest {
    @Test
    fun `accepts first last and intermediate pages`() {
        for (page in listOf(1, 8, 1657)) {
            assertEquals(page, parseFeedPageNumber(page.toString(), 1657))
        }
        assertEquals(8, parseFeedPageNumber(" 008 ", 1657))
        assertEquals(1, parseFeedPageNumber("1", 1))
    }

    @Test
    fun `rejects empty and non integer input`() {
        for (input in listOf("", " ", "abc", "2.5", "1e2", "+2", "-1", "1 2", "2\n3")) {
            assertNull(parseFeedPageNumber(input, 1657), input)
        }
    }

    @Test
    fun `rejects pages outside the available range`() {
        for (input in listOf("0", "000", "1658", Int.MAX_VALUE.toString())) {
            assertNull(parseFeedPageNumber(input, 1657), input)
        }
        assertNull(parseFeedPageNumber("1", 0))
    }

    @Test
    fun `handles integer overflow without throwing`() {
        assertEquals(Int.MAX_VALUE, parseFeedPageNumber(Int.MAX_VALUE.toString(), Int.MAX_VALUE))
        assertNull(parseFeedPageNumber("2147483648", Int.MAX_VALUE))
        assertNull(parseFeedPageNumber("9".repeat(100), Int.MAX_VALUE))
    }
}
