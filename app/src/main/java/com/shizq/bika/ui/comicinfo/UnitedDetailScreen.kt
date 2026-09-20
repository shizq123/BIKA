package com.shizq.bika.ui.comicinfo

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.ui.ErrorState
import com.shizq.bika.core.ui.LoadingState
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.ui.comicinfo.page.ComicDetailPage
import com.shizq.bika.ui.comicinfo.page.CommentsTab
import com.shizq.bika.ui.comicinfo.page.EpisodesPage
import com.shizq.bika.ui.comicinfo.page.PageTab
import kotlinx.coroutines.launch

@Composable
fun ComicDetailScreen(
    navigationToReader: (id: String, index: Int) -> Unit,
    onForYouClick: (String) -> Unit,
    onBackClick: () -> Unit,
    navigationToFeed: (DiscoveryAction) -> Unit,
    viewModel: ComicInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val episodes = viewModel.episodesFlow.collectAsLazyPagingItems()
    val downloadTasks by viewModel.downloadTasks.collectAsStateWithLifecycle()
    val chapterProgress by viewModel.chapterProgress.collectAsStateWithLifecycle()

    ComicDetailContent(
        unitedState = state,
        episodes = episodes,
        downloadTasks = downloadTasks,
        chapterProgress = chapterProgress,
        onBackClick = onBackClick,
        navigationToReader = navigationToReader,
        navigationToComicInfo = onForYouClick,
        dispatch = viewModel::dispatch,
        navigationToFeed = navigationToFeed,
        onFetchAllEpisodes = { viewModel.fetchAllEpisodes() },
        onDownloadAllEpisodes = { title, cover ->
            viewModel.downloadAllEpisodes(title, cover)
        },
        onDownloadEpisodes = { title, cover, list ->
            viewModel.downloadEpisodes(title, cover, list)
        },
        onTagBlocked = viewModel::addBlockedTag,
    )
}

// 参数没有默认值是有意的：`= {}` / `= { _, _ -> 0 }` 会让"漏接一根回调"
// 变成编译期沉默的错误，只能靠人眼比对 ComicDetailScreen 里的赋值列表。
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicDetailContent(
    unitedState: UnitedDetailsUiState,
    episodes: LazyPagingItems<Chapter>,
    downloadTasks: List<DownloadTask>,
    chapterProgress: List<ChapterProgressEntity>,
    onBackClick: () -> Unit,
    navigationToReader: (id: String, index: Int) -> Unit,
    navigationToComicInfo: (String) -> Unit,
    dispatch: (UnitedDetailsAction) -> Unit,
    navigationToFeed: (DiscoveryAction) -> Unit,
    onFetchAllEpisodes: suspend () -> List<Chapter>,
    onDownloadAllEpisodes: suspend (String, String) -> Int,
    onDownloadEpisodes: (String, String, List<Chapter>) -> Unit,
    onTagBlocked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (unitedState) {
        is UnitedDetailsUiState.Initialize -> LoadingState()
        is UnitedDetailsUiState.Error -> ErrorState({ dispatch(UnitedDetailsAction.Retry) })

        is UnitedDetailsUiState.Content -> {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

            val detail = unitedState.detail

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            IconButton(onBackClick) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                },
                modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    val pagerState = rememberPagerState { PageTab.entries.size }
                    val scope = rememberCoroutineScope()

                    PrimaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                    ) {
                        PageTab.entries.forEachIndexed { index, tab ->
                            Tab(
                                selected = index == pagerState.currentPage,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = { Text(text = tab.title) }
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top,
                        key = { it }
                    ) { page ->
                        val context = LocalContext.current
                        when (PageTab.entries[page]) {
                            PageTab.DETAIL -> {
                                // remember 避免每次重组都重算一遍整个任务列表
                                val isComicDownloaded = remember(downloadTasks, detail.epsCount) {
                                    isComicFullyDownloaded(downloadTasks, detail.epsCount)
                                }

                                val lastReadChapter = remember(chapterProgress) {
                                    chapterProgress.maxByOrNull { it.lastReadAt }
                                }
                                val lastReadChapterOrder = lastReadChapter?.chapterId ?: 1
                                val isContinue = lastReadChapter != null

                                var tagToBlock by remember { mutableStateOf<String?>(null) }

                                ComicDetailPage(
                                    detail = detail,
                                    recommendations = unitedState.recommendations,
                                    isDownloaded = isComicDownloaded,
                                    isContinue = isContinue,
                                    onFavoriteClick = { dispatch(UnitedDetailsAction.ToggleFavorite) },
                                    onLikedClick = { dispatch(UnitedDetailsAction.ToggleLike) },
                                    navigationToReader = { navigationToReader(detail.id, lastReadChapterOrder) },
                                    navigationToComicInfo = { navigationToComicInfo(it) },
                                    navigationToFeed = navigationToFeed,
                                    onDownloadClick = {
                                        if (detail.epsCount <= 1) {
                                            // 单话漫画直接走 ViewModel 入队
                                            onDownloadEpisodes(
                                                detail.title,
                                                detail.cover,
                                                listOf(
                                                    Chapter(
                                                        id = "single_episode",
                                                        title = "全一话",
                                                        order = 1,
                                                        updatedAt = ""
                                                    )
                                                )
                                            )
                                            Toast.makeText(context, "已加入下载队列", Toast.LENGTH_SHORT).show()
                                        } else {
                                            scope.launch {
                                                try {
                                                    Toast.makeText(context, "正在获取章节列表，准备全部下载...", Toast.LENGTH_SHORT).show()
                                                    val count = onDownloadAllEpisodes(
                                                        detail.title,
                                                        detail.cover
                                                    )
                                                    Toast.makeText(context, "已成功将所有 $count 个章节加入下载队列", Toast.LENGTH_LONG).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "获取章节失败，请重试: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    onTagLongClick = { tagToBlock = it }
                                )

                                if (tagToBlock != null) {
                                    AlertDialog(
                                        onDismissRequest = { tagToBlock = null },
                                        title = { Text("屏蔽标签") },
                                        text = { Text("确认屏蔽标签“${tagToBlock}”吗？屏蔽后，所有含有此标签的漫画将不再展示在列表中。") },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    tagToBlock?.let { onTagBlocked(it) }
                                                    Toast.makeText(context, "已屏蔽标签“${tagToBlock}”", Toast.LENGTH_SHORT).show()
                                                    tagToBlock = null
                                                }
                                            ) {
                                                Text("确定")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { tagToBlock = null }) {
                                                Text("取消")
                                            }
                                        }
                                    )
                                }
                            }

                            PageTab.EPISODES -> {
                                EpisodesPage(
                                    episodes = episodes,
                                    downloadTasks = downloadTasks,
                                    chapterProgress = chapterProgress,
                                    navigateToReader = {
                                        navigationToReader(detail.id, it)
                                    },
                                    onDownloadClick = { selectedEpisodes ->
                                        onDownloadEpisodes(
                                            detail.title,
                                            detail.cover,
                                            selectedEpisodes
                                        )
                                        val message = if (selectedEpisodes.size == 1) {
                                            "已加入下载队列"
                                        } else {
                                            "已成功将 ${selectedEpisodes.size} 个章节加入下载队列"
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    },
                                    onFetchAllEpisodes = onFetchAllEpisodes
                                )
                            }

                            PageTab.COMMENT -> CommentsTab(comicId = detail.id)
                        }
                    }
                }
            }
        }
    }
}


