package com.shizq.bika.ui.comicinfo

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.core.ui.ErrorState
import com.shizq.bika.core.ui.LoadingState
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.ui.comicinfo.page.ComicDetailPage
import com.shizq.bika.ui.comicinfo.page.CommentsPage
import com.shizq.bika.ui.comicinfo.page.EpisodeItem
import com.shizq.bika.ui.comicinfo.page.EpisodesPage
import com.shizq.bika.ui.comicinfo.page.PageTab
import kotlinx.coroutines.launch

@Composable
fun ComicDetailScreen(
    viewModel: ComicInfoViewModel = hiltViewModel(),
    navigationToReader: (id: String, index: Int) -> Unit,
    onForYouClick: (String) -> Unit,
    onBackClick: () -> Unit,
    navigationToFeed: (DiscoveryAction) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val episodes = viewModel.episodesFlow.collectAsLazyPagingItems()
    val downloadTasks by viewModel.downloadTasks.collectAsStateWithLifecycle()
    val chapterProgress by viewModel.chapterProgress.collectAsStateWithLifecycle()

    val pinnedComments by viewModel.pinnedComments.collectAsStateWithLifecycle()
    val regularComments = viewModel.regularComments.collectAsLazyPagingItems()

    val replyList = viewModel.replyList.collectAsLazyPagingItems()
    ComicDetailContent(
        unitedState = state,
        episodes = episodes,
        downloadTasks = downloadTasks,
        chapterProgress = chapterProgress,
        onBackClick = onBackClick,
        navigationToReader = navigationToReader,
        navigationToComicInfo = onForYouClick,
        pinnedComments = pinnedComments,
        regularComments = regularComments,
        onToggleCommentLike = viewModel::toggleCommentLike,
        dispatch = viewModel::dispatch,
        replyList = replyList,
        navigationToFeed = navigationToFeed,
        onFetchAllEpisodes = { viewModel.fetchAllEpisodes() },
        onDownloadAllEpisodes = { title, cover ->
            viewModel.downloadAllEpisodes(title, cover)
        },
        onDownloadEpisodes = { title, cover, list ->
            viewModel.downloadEpisodes(title, cover, list)
        },
        onPostComment = viewModel::postComment,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicDetailContent(
    unitedState: UnitedDetailsUiState,
    episodes: LazyPagingItems<Episode>,
    pinnedComments: List<Comment>,
    regularComments: LazyPagingItems<Comment>,
    downloadTasks: List<DownloadTask> = emptyList(),
    chapterProgress: List<ChapterProgressEntity> = emptyList(),
    onBackClick: () -> Unit = {},
    navigationToReader: (id: String, index: Int) -> Unit = { _, _ -> },
    navigationToComicInfo: (String) -> Unit = {},
    onToggleCommentLike: (String) -> Unit = {},
    dispatch: (UnitedDetailsAction) -> Unit = {},
    replyList: LazyPagingItems<Comment>,
    navigationToFeed: (DiscoveryAction) -> Unit,
    onFetchAllEpisodes: suspend () -> List<Episode> = { emptyList() },
    onDownloadAllEpisodes: suspend (String, String) -> Int = { _, _ -> 0 },
    onDownloadEpisodes: (String, String, List<Episode>) -> Unit = { _, _, _ -> },
    onPostComment: (text: String, replyToCommentId: String?, onResult: (Boolean) -> Unit) -> Unit = { _, _, _ -> },
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
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                        when (page) {
                            0 -> {
                                val isComicDownloaded = if (detail.epsCount <= 1) {
                                    downloadTasks.any { it.status == DownloadStatus.COMPLETED }
                                } else {
                                    val completedCount =
                                        downloadTasks.count { it.status == DownloadStatus.COMPLETED }
                                    completedCount > 0 && completedCount >= detail.epsCount
                                }

                                val lastReadChapter = remember(chapterProgress) {
                                    chapterProgress.maxByOrNull { it.lastReadAt }
                                }
                                val lastReadChapterOrder = lastReadChapter?.chapterId ?: 1
                                val isContinue = lastReadChapter != null

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
                                                    Episode(
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
                                    }
                                )
                            }

                            1 -> {
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

                            2 -> CommentsPage(
                                pinnedComments = pinnedComments,
                                regularComments = regularComments,
                                onToggleCommentLike = onToggleCommentLike,
                                replyList = replyList,
                                onExpandReplies = {
                                    dispatch(
                                        UnitedDetailsAction.ExpandReplies(
                                            it
                                        )
                                    )
                                },
                                onPostComment = { text, replyToCommentId ->
                                    onPostComment(text, replyToCommentId) { success ->
                                        if (success) {
                                            regularComments.refresh()
                                            if (replyToCommentId != null) {
                                                replyList.refresh()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EpisodeItemPreview() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 80.dp),
        modifier = Modifier.heightIn(max = 400.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(count = 30) { index ->
            EpisodeItem(
                text = "第${index}话",
                onClick = { },
            )
        }
    }
}
