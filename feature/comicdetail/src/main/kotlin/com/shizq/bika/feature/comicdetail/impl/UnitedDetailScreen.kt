package com.shizq.bika.feature.comicdetail.impl

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.ui.ComicSharedElementKey
import com.shizq.bika.core.ui.ErrorState
import com.shizq.bika.core.ui.LoadingState
import com.shizq.bika.core.ui.RetryableAsyncImage
import com.shizq.bika.feature.comicdetail.impl.comments.CommentsTab
import com.shizq.bika.feature.comicdetail.impl.detail.DetailTab
import com.shizq.bika.feature.comicdetail.impl.detail.PageTab
import com.shizq.bika.feature.comicdetail.impl.episodes.EpisodesPage
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ComicDetailScreen(
    navigationToReader: (id: String, index: Int) -> Unit,
    onForYouClick: (String) -> Unit,
    navigationToFeed: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onUploaderClick: (String, String) -> Unit,
    navigationToEpisodeDownload: (String, String, String) -> Unit,
    onBackClick: () -> Unit,
    previewCoverUrl: String? = null,
    previewTitle: String? = null,
    coverSharedElementKey: ComicSharedElementKey? = null,
    titleSharedElementKey: ComicSharedElementKey? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    viewModel: ComicInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val episodes = viewModel.episodesFlow.collectAsLazyPagingItems()
    val downloadTasks by viewModel.downloadTasks.collectAsStateWithLifecycle()
    val chapterProgress by viewModel.chapterProgress.collectAsStateWithLifecycle()

    ComicDetailContent(
        unitedState = state,
        previewCoverUrl = previewCoverUrl,
        previewTitle = previewTitle,
        coverSharedElementKey = coverSharedElementKey,
        titleSharedElementKey = titleSharedElementKey,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        episodes = episodes,
        chapterProgress = chapterProgress,
        onBackClick = onBackClick,
        navigationToReader = navigationToReader,
        onRecommendedComicClick = onForYouClick,
        dispatch = viewModel::dispatch,
        onTranslateClick = navigationToFeed,
        onDownloadWholeComic = { title, cover, epsCount ->
            viewModel.downloadWholeComic(title, cover, epsCount)
        },
        onTagClick = onTagClick,
        navigationToEpisodeDownload = navigationToEpisodeDownload,
        downloadTasks = downloadTasks,
        onAuthorClick = onAuthorClick,
        onUploaderClick = onUploaderClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ComicDetailContent(
    unitedState: UnitedDetailsUiState,
    episodes: LazyPagingItems<Chapter>,
    downloadTasks: List<DownloadTask>,
    chapterProgress: List<ChapterProgressEntity>,
    onBackClick: () -> Unit,
    navigationToReader: (id: String, index: Int) -> Unit,
    onRecommendedComicClick: (String) -> Unit,
    dispatch: (UnitedDetailsAction) -> Unit,
    onTranslateClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onUploaderClick: (String, String) -> Unit,
    onDownloadWholeComic: (title: String, cover: String, epsCount: Int) -> Unit,
    onTagClick: (String) -> Unit,
    navigationToEpisodeDownload: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
    previewCoverUrl: String? = null,
    previewTitle: String? = null,
    coverSharedElementKey: ComicSharedElementKey? = null,
    titleSharedElementKey: ComicSharedElementKey? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val sharedCoverModifier = if (
        sharedTransitionScope != null &&
        animatedVisibilityScope != null &&
        coverSharedElementKey != null
    ) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(coverSharedElementKey),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }

    val sharedTitleModifier = if (
        sharedTransitionScope != null &&
        animatedVisibilityScope != null &&
        titleSharedElementKey != null
    ) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(titleSharedElementKey),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }

    when (unitedState) {
        is UnitedDetailsUiState.Initialize -> ComicDetailTransitionPlaceholder(
            coverUrl = previewCoverUrl,
            title = previewTitle,
            coverModifier = sharedCoverModifier,
            titleModifier = sharedTitleModifier,
            onBackClick = onBackClick,
            modifier = modifier,
        )

        is UnitedDetailsUiState.Error -> ErrorState {
            dispatch(UnitedDetailsAction.Retry)
        }

        is UnitedDetailsUiState.Content -> {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            val detail = unitedState.detail

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = detail.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                            }
                        },
                        scrollBehavior = scrollBehavior,
                    )
                },
                modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                ) {
                    val pagerState = rememberPagerState { PageTab.entries.size }
                    val scope = rememberCoroutineScope()

                    PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                        PageTab.entries.forEachIndexed { index, tab ->
                            Tab(
                                selected = index == pagerState.currentPage,
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                text = { Text(text = tab.title) },
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top,
                        key = { it },
                    ) { page ->
                        when (PageTab.entries[page]) {
                            PageTab.DETAIL -> DetailTab(
                                detail = detail,
                                recommendations = unitedState.recommendations,
                                downloadTasks = downloadTasks,
                                chapterProgress = chapterProgress,
                                coverModifier = sharedCoverModifier,
                                titleModifier = sharedTitleModifier,
                                onFavoriteClick = {
                                    dispatch(UnitedDetailsAction.ToggleFavorite)
                                },
                                onLikedClick = { dispatch(UnitedDetailsAction.ToggleLike) },
                                navigationToReader = { navigationToReader(detail.id, it) },
                                onRecommendedComicClick = onRecommendedComicClick,
                                onTranslateClick = onTranslateClick,
                                onDownloadWholeComic = {
                                    onDownloadWholeComic(
                                        detail.title,
                                        detail.cover,
                                        detail.epsCount,
                                    )
                                },
                                navigationToTagBlock = onTagClick,
                                onAuthorClick = onAuthorClick,
                                onUploaderClick = onUploaderClick,
                            )

                            PageTab.EPISODES -> EpisodesPage(
                                episodes = episodes,
                                chapterProgress = chapterProgress,
                                navigateToReader = {
                                    navigationToReader(detail.id, it)
                                },
                                onDownloadSelectionClick = {
                                    navigationToEpisodeDownload(
                                        detail.id,
                                        detail.title,
                                        detail.cover,
                                    )
                                },
                            )

                            PageTab.COMMENT -> CommentsTab(comicId = detail.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComicDetailTransitionPlaceholder(
    coverUrl: String?,
    title: String?,
    coverModifier: Modifier,
    titleModifier: Modifier,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (coverUrl == null || title == null) {
        LoadingState(modifier)
        return
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            // 与真实详情内容保持相同的 Tab 行高度和 Hero 布局坐标，避免数据加载完成后
            // 共享元素先落到页面左上角，再跳到 HeroHeader 的最终位置。
            PrimaryTabRow(selectedTabIndex = 0) {
                PageTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == 0,
                        onClick = {},
                        text = { Text(text = tab.title) },
                    )
                }
            }

            ComicDetailHeroPlaceholder(
                coverUrl = coverUrl,
                title = title,
                coverModifier = coverModifier,
                titleModifier = titleModifier,
            )
        }
    }
}

@Composable
private fun ComicDetailHeroPlaceholder(
    coverUrl: String,
    title: String,
    coverModifier: Modifier,
    titleModifier: Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(318.dp),
    ) {
        RetryableAsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            RetryableAsyncImage(
                model = coverUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = coverModifier
                    .width(122.dp)
                    .height(166.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = titleModifier
                    .weight(1f)
                    .padding(start = 18.dp),
            )
        }
    }
}
