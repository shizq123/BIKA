package com.shizq.bika.ui.comicinfo.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.network.model.ComicData.Comic.Creator
import com.shizq.bika.core.network.model.Media
import com.shizq.bika.core.ui.RetryableAsyncImage
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.ui.comicinfo.ComicDetail
import com.shizq.bika.ui.comicinfo.ComicSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ComicDetailPageTest {

    @get:Rule
    val composeRule = createComposeRule()

    // 该图 URL 在本模拟器环境已验证可加载（logcat 中 RealImageLoader Successful）。
    // 必须让 RetryableAsyncImage 加载成功：任何失败都会进入无限退避重试协程，
    // 导致 Espresso 判定 Compose “永不空闲”而超时（ComposeNotIdleException）。
    private val validCover =
        "https://storage1.picacomic.com/static/tobeimg/7c0QXuFh2AWN9OaywE3MeITazz08h4MnpEUgUlWStzU/" +
            "rs:fill:300:400:0/g:sm/aHR0cHM6Ly9zdG9yYWdlMS5waWNhY29taWMuY29tL3N0YXRpYy9jZmFl" +
            "MWUxZi0zMmZkLTQ2ZmMtODFkYi0zZTMwZThhNzZkNDQuanBn.jpg"

    private val creator = Creator(
        avatar = Media(
            path = "tobeimg/7c0QXuFh2AWN9OaywE3MeITazz08h4MnpEUgUlWStzU/rs:fill:300:400:0/g:sm/" +
                "aHR0cHM6Ly9zdG9yYWdlMS5waWNhY29taWMuY29tL3N0YXRpYy9jZmFlMWUxZi0zMmZkLTQ2" +
                "ZmMtODFkYi0zZTMwZThhNzZkNDQuanBn.jpg",
            fileServer = "https://storage1.picacomic.com",
        ),
        name = "测试创作者",
    )

    private val detail = ComicDetail(
        id = "comic-1",
        title = "测试漫画",
        author = "测试作者",
        cover = validCover,
        creator = creator,
        description = "这是一段测试简介",
    )

    private val recommendations = listOf(
        ComicSummary("rec-1", "推荐漫画一", validCover, "作者A"),
        ComicSummary("rec-2", "推荐漫画二", validCover, "作者B"),
        ComicSummary(
            "rec-3",
            "这是一个非常非常长的推荐漫画标题用来验证名称是否会被截断显示",
            validCover,
            "作者C",
        ),
    )

    private fun setPage(onNavigate: (String) -> Unit) {
        composeRule.setContent {
            MaterialTheme {
                ComicDetailPage(
                    detail = detail,
                    recommendations = recommendations,
                    navigationToComicInfo = onNavigate,
                    navigationToFeed = { _: DiscoveryAction -> },
                    onFavoriteClick = {},
                    onLikedClick = {},
                    navigationToReader = {},
                    onDownloadClick = {},
                )
            }
        }
    }

    // ============ 用户场景：完整漫画详情页 ============

    @Test
    fun recommendationCards_showTitle_and_semanticClickNavigates() {
        val clickedId = java.util.concurrent.atomic.AtomicReference<String?>(null)
        setPage { clickedId.set(it) }

        // 轮播在页面下方，先滚动到推荐区域
        composeRule.onNodeWithText("推荐漫画一").performScrollTo().assertIsDisplayed()

        // 推荐卡片标题必须显示（用户可见）
        composeRule.onNodeWithText("推荐漫画二").assertExists()
        composeRule.onNodeWithText(
            "这是一个非常非常长的推荐漫画标题用来验证名称是否会被截断显示"
        ).assertExists()

        // 语义点击：等价于 clickable 的 onClick 语义触发（不经过触摸命中）
        composeRule.onNodeWithText("推荐漫画一").performClick()
        assertEquals("rec-1", clickedId.get())
    }

    @Test
    fun recommendationCard_realTouchClickNavigates() {
        val clickedId = java.util.concurrent.atomic.AtomicReference<String?>(null)
        setPage { clickedId.set(it) }

        // 模拟真实手指触摸（走 pointer input 命中测试）
        composeRule.onNodeWithText("推荐漫画一").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("推荐漫画一").performTouchInput { click() }
        assertEquals("rec-1", clickedId.get())
    }

    // ============ 结构对照：定位真实触摸失效元凶 ============
    // A: Column(clickable){Text} —— 基线（已通过）
    // D: Column(clickable){ Box(205dp) + Text } —— 高度占位
    // E: Column(clickable){ AsyncImage(205dp) + Text } —— 裸图片
    // F: Column(clickable){ RetryableAsyncImage(205dp) + Text } —— 完整结构
    // 全部点击卡片中心（封面区域），模拟用户点击封面

    @Test
    fun structuredRepro_realTouch_inCarousel() {
        val dClicked = java.util.concurrent.atomic.AtomicReference<String?>(null)
        val eClicked = java.util.concurrent.atomic.AtomicReference<String?>(null)
        val fClicked = java.util.concurrent.atomic.AtomicReference<String?>(null)
        composeRule.setContent {
            MaterialTheme {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // D：占位 Box
                    HorizontalMultiBrowseCarousel(
                        state = rememberCarouselState { 3 },
                        preferredItemWidth = 186.dp,
                        modifier = Modifier.fillMaxWidth(),
                        itemSpacing = 8.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) { index ->
                        Column(
                            Modifier.clickable { dClicked.set("d-$index") }
                        ) {
                            androidx.compose.foundation.layout.Box(Modifier.height(205.dp))
                            Text("D 项 $index")
                        }
                    }
                    // E：裸 AsyncImage
                    HorizontalMultiBrowseCarousel(
                        state = rememberCarouselState { 3 },
                        preferredItemWidth = 186.dp,
                        modifier = Modifier.fillMaxWidth(),
                        itemSpacing = 8.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) { index ->
                        Column(
                            Modifier.clickable { eClicked.set("e-$index") }
                        ) {
                            coil3.compose.AsyncImage(
                                model = validCover,
                                contentDescription = "封面-$index",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.height(205.dp)
                            )
                            Text("E 项 $index")
                        }
                    }
                    // F：RetryableAsyncImage（完整结构）
                    HorizontalMultiBrowseCarousel(
                        state = rememberCarouselState { 1 },
                        preferredItemWidth = 186.dp,
                        modifier = Modifier.fillMaxWidth(),
                        itemSpacing = 8.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) { index ->
                        Column(
                            Modifier.clickable { fClicked.set("f-$index") }
                        ) {
                            RetryableAsyncImage(
                                validCover,
                                contentDescription = "封面-$index",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.height(205.dp)
                            )
                            Text("F 项 $index")
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("D 项 0").assertIsDisplayed()
        composeRule.onNodeWithText("D 项 0").performTouchInput { click() }
        assertEquals("d-0", dClicked.get())

        composeRule.onNodeWithText("E 项 0").performTouchInput { click() }
        assertEquals("e-0", eClicked.get())

        composeRule.onNodeWithText("F 项 0").performTouchInput { click() }
        assertEquals("f-0", fClicked.get())
    }
}
