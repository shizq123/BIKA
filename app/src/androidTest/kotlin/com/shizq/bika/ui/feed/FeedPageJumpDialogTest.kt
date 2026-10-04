package com.shizq.bika.ui.feed

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FeedPageJumpDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun invalidPage_showsValidationError() {
        setDialog(totalPages = 3)

        composeRule.onNodeWithTag(FeedPageJumpTestTags.Input).performTextReplacement("4")
        composeRule.onNodeWithText("跳转").performClick()

        composeRule.onNodeWithText("页码不能超过 3").assertIsDisplayed()
    }

    @Test
    fun validPage_confirmsNumericValue() {
        val confirmed = mutableListOf<Int>()
        setDialog(totalPages = 3, onConfirm = confirmed::add)

        composeRule.onNodeWithTag(FeedPageJumpTestTags.Input).performTextReplacement("2")
        composeRule.onNodeWithText("跳转").performClick()

        assertEquals(listOf(2), confirmed)
    }

    private fun setDialog(
        totalPages: Int,
        onConfirm: (Int) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                FeedPageJumpDialog(
                    currentPage = 1,
                    totalPages = totalPages,
                    onConfirm = onConfirm,
                    onDismiss = {},
                )
            }
        }
    }
}
