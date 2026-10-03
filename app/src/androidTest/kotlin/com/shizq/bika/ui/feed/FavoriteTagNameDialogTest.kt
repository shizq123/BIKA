package com.shizq.bika.ui.feed

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoriteTagNameDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun blankName_disablesConfirm() {
        setDialog(initialValue = "   ")

        composeRule.onNodeWithText("保存").assertIsNotEnabled()
    }

    @Test
    fun confirm_trimsNameBeforeCallback() {
        val confirmed = mutableListOf<String>()
        setDialog(onConfirm = confirmed::add)

        composeRule.onNodeWithTag(FavoriteTagNameTestTags.INPUT)
            .performTextReplacement("  我的标签  ")
        composeRule.onNodeWithText("保存").assertIsEnabled().performClick()

        assertEquals(listOf("我的标签"), confirmed)
    }

    private fun setDialog(
        initialValue: String = "",
        onConfirm: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                FavoriteTagNameDialog(
                    title = "标题",
                    label = "名称",
                    confirmText = "保存",
                    initialValue = initialValue,
                    onConfirm = onConfirm,
                    onDismiss = {},
                )
            }
        }
    }
}
