package com.shizq.bika.ui.comicinfo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.shizq.bika.ui.comicinfo.page.CommentsTab
import com.shizq.bika.ui.comicinfo.page.DetailTab
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
        onLoadSelectableEpisodes = { viewModel.loadSelectableEpisodes() },
        onDownloadWholeComic = { title, cover, epsCount ->
            viewModel.downloadWholeComic(title, cover, epsCount)
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
    onLoadSelectableEpisodes: suspend () -> List<Chapter>?,
    onDownloadWholeComic: (title: String, cover: String, epsCount: Int) -> Unit,
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
                        when (PageTab.entries[page]) {
                            PageTab.DETAIL -> DetailTab(
                                detail = detail,
                                recommendations = unitedState.recommendations,
                                downloadTasks = downloadTasks,
                                chapterProgress = chapterProgress,
                                onFavoriteClick = { dispatch(UnitedDetailsAction.ToggleFavorite) },
                                onLikedClick = { dispatch(UnitedDetailsAction.ToggleLike) },
                                navigationToReader = { navigationToReader(detail.id, it) },
                                navigationToComicInfo = navigationToComicInfo,
                                navigationToFeed = navigationToFeed,
                                onDownloadWholeComic = {
                                    onDownloadWholeComic(
                                        detail.title,
                                        detail.cover,
                                        detail.epsCount,
                                    )
                                },
                                onTagBlocked = onTagBlocked,
                            )

                            PageTab.EPISODES -> EpisodesPage(
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
                                        selectedEpisodes,
                                    )
                                },
                                onLoadSelectableEpisodes = onLoadSelectableEpisodes,
                            )

                            PageTab.COMMENT -> CommentsTab(comicId = detail.id)
                        }
                    }
                }
            }
        }
    }
}


