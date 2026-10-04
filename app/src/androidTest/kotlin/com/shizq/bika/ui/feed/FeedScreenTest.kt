package com.shizq.bika.ui.feed

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FeedScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyPagingData_showsEmptyState() {
        setFeed()

        composeRule.onNodeWithTag(FeedTestTags.Empty).assertIsDisplayed()
    }

    @Test
    fun backButton_navigatesBack() {
        val destinations = mutableListOf<FeedDestination>()
        setFeed(onNavigate = destinations::add)

        composeRule.onNodeWithTag(FeedTestTags.Back).performClick()

        assertEquals(listOf(FeedDestination.Back), destinations)
    }

    private fun setFeed(
        state: FeedUiState = stateWithContent(),
        onAction: (FeedAction) -> Unit = {},
        onNavigate: (FeedDestination) -> Unit = {},
    ) {
        composeRule.setContent {
            val comics = flowOf(PagingData.empty<com.shizq.bika.core.model.ComicSummary>())
                .collectAsLazyPagingItems()
            MaterialTheme {
                FeedScreen(
                    title = "测试列表",
                    state = state,
                    comics = comics,
                    currentTag = null,
                    onAction = onAction,
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun stateWithContent(): FeedUiState = FeedUiState.Content(
        data = FeedData(),
    )
}
