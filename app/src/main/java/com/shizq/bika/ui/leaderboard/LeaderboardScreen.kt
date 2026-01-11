package com.shizq.bika.ui.leaderboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.model.ComicSimple
import com.shizq.bika.core.ui.ComicCard
import kotlinx.coroutines.launch


@Composable
fun LeaderboardScreen(
    navigationToUnitedDetail: (String) -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel(),
    navigationToKnight: (String, String) -> Unit
) {
    val leaderboardState by viewModel.leaderboardUiState.collectAsStateWithLifecycle()
    LeaderboardContent(
        leaderboardState,
        navigationToUnitedDetail = navigationToUnitedDetail,
        navigationToKnight = navigationToKnight,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardContent(
    leaderboardState: LeaderboardUiState,
    navigationToUnitedDetail: (String) -> Unit,
    navigationToKnight: (String, String) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val pagerState = rememberPagerState(pageCount = { LEADERBOARD_TABS.size })

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LeaderboardTopAppBar(
                pagerState = pagerState,
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (leaderboardState) {
                is LeaderboardUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is LeaderboardUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "加载失败: ${leaderboardState.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { /* 重试逻辑需在 VM 添加 */ },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重试")
                        }
                    }
                }

                is LeaderboardUiState.Success -> {
                    LeaderboardPagerContent(
                        pagerState = pagerState,
                        successState = leaderboardState,
                        navigationToUnitedDetail = navigationToUnitedDetail,
                        navigationToKnight = navigationToKnight,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LeaderboardPagerContent(
    pagerState: PagerState,
    successState: LeaderboardUiState.Success,
    navigationToUnitedDetail: (String) -> Unit,
    navigationToKnight: (String, String) -> Unit,
) {
    HorizontalPager(state = pagerState) { page ->
        when (page) {
            0 -> ComicLeaderboardPage(
                list = successState.dailyList,
                navigationToUnitedDetail = navigationToUnitedDetail
            )

            1 -> ComicLeaderboardPage(
                list = successState.weeklyList,
                navigationToUnitedDetail = navigationToUnitedDetail
            )

            2 -> ComicLeaderboardPage(
                list = successState.monthlyList,
                navigationToUnitedDetail = navigationToUnitedDetail
            )

            3 -> KnightLeaderboardPage(
                knightList = successState.knightList,
                navigationToKnight = navigationToKnight
            )
            else -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("页面不存在")
            }
        }
    }
}

@Composable
fun ComicLeaderboardPage(
    list: List<ComicSimple>,
    modifier: Modifier = Modifier,
    navigationToUnitedDetail: (String) -> Unit
) {
    if (list.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无数据")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(list, key = { it.id }) { item ->
                ComicCard(
                    item,
                    onItemClick = { navigationToUnitedDetail(item.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LeaderboardTopAppBar(
    pagerState: PagerState,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val coroutineScope = rememberCoroutineScope()

    Column {
        CenterAlignedTopAppBar(
            title = { Text("热门排行") },
            navigationIcon = {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = "排行榜图标",
                    modifier = Modifier.padding(start = 16.dp)
                )
            },
            scrollBehavior = scrollBehavior
        )
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            LEADERBOARD_TABS.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    }
}

private val LEADERBOARD_TABS = listOf("日榜 (24H)", "周榜 (7D)", "月榜 (30D)", "骑士榜")