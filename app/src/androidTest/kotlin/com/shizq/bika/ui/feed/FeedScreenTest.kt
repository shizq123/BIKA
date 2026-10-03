package com.shizq.bika.ui.feed

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.shizq.bika.core.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FeedScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialState_showsLoading_andBackNavigates() {
        val destinations = mutableListOf<FeedDestination>()
        setFeed(state = FeedUiState(), onNavigate = destinations::add)

        composeRule.onNodeWithTag(FeedTestTags.InitialLoading).assertIsDisplayed()
        composeRule.onNodeWithTag(FeedTestTags.Back).performClick()

        assertEquals(listOf(FeedDestination.Back), destinations)
    }

    @Test
    fun emptySuccess_showsEmptyState() {
        setFeed(state = stateWithSuccess())

        composeRule.onNodeWithTag(FeedTestTags.Empty).assertIsDisplayed()
    }

    @Test
    fun refreshingExistingPage_showsProgress_andDisablesPagination() {
        val targetQuery = FeedQuery(page = 2, sort = SortOrder.NEWEST)
        setFeed(
            state = stateWithSuccess(
                refreshState = FeedRefreshState.Loading(targetQuery),
            )
        )

        composeRule.onNodeWithTag(FeedTestTags.RefreshProgress).assertIsDisplayed()
        composeRule.onNodeWithTag(FeedTestTags.List)
            .performScrollToNode(hasTestTag(FeedTestTags.Pagination))
        composeRule.onNodeWithTag(FeedTestTags.Pagination).assertExists()
        composeRule.onNodeWithContentDescription("下一页").assertIsNotEnabled()
    }

    @Test
    fun refreshFailure_keepsContentAreaAndShowsRetryBanner() {
        val actions = mutableListOf<FeedAction>()
        setFeed(
            state = stateWithSuccess(
                refreshState = FeedRefreshState.Failed(
                    targetQuery = FeedQuery(page = 2),
                    error = FeedError.LoadFailed,
                ),
            ),
            onAction = actions::add,
        )

        composeRule.onNodeWithTag(FeedTestTags.RefreshError).assertIsDisplayed()
        composeRule.onNodeWithText("重试").performClick()

        assertEquals(listOf(FeedAction.Retry), actions)
    }

    private fun setFeed(
        state: FeedUiState,
        onAction: (FeedAction) -> Unit = {},
        onNavigate: (FeedDestination) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                FeedScreen(
                    title = "测试列表",
                    state = state,
                    currentTag = null,
                    onAction = onAction,
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun stateWithSuccess(
        refreshState: FeedRefreshState = FeedRefreshState.Idle,
    ): FeedUiState = FeedUiState(
        query = FeedQuery(page = 1),
        content = FeedContentState.Success(
            page = FeedPage(
                items = emptyList(),
                page = 1,
                totalPages = 3,
                totalCount = 0,
            ),
            displayedQuery = FeedQuery(page = 1),
            refreshState = refreshState,
        ),
    )
}
