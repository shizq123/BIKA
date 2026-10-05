package com.shizq.bika.feature.comicdetail.impl

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.shizq.bika.core.ui.RetryableAsyncImage
import com.shizq.bika.feature.comicdetail.impl.comments.CommentsTab
import com.shizq.bika.feature.comicdetail.impl.detail.DetailTab
import com.shizq.bika.feature.comicdetail.impl.detail.PageTab
import com.shizq.bika.feature.comicdetail.impl.episodes.EpisodesPage
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
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
    previewCoverUrl: String,
    previewTitle: String,
    coverSharedElementKey: ComicSharedElementKey,
    titleSharedElementKey: ComicSharedElementKey,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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
    previewCoverUrl: String,
    previewTitle: String,
    coverSharedElementKey: ComicSharedElementKey,
    titleSharedElementKey: ComicSharedElementKey,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val sharedCoverModifier = with(sharedTransitionScope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(coverSharedElementKey),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }

    val sharedTitleModifier = with(sharedTransitionScope) {
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(titleSharedElementKey),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }

    when (unitedState) {
        UnitedDetailsUiState.Initialize -> ComicDetailInitializeContent(
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

        is UnitedDetailsUiState.Content -> ComicDetailLoadedContent(
            state = unitedState,
            episodes = episodes,
            downloadTasks = downloadTasks,
            chapterProgress = chapterProgress,
            previewTitle = previewTitle,
            coverModifier = sharedCoverModifier,
            titleModifier = sharedTitleModifier,
            onBackClick = onBackClick,
            navigationToReader = navigationToReader,
            onRecommendedComicClick = onRecommendedComicClick,
            dispatch = dispatch,
            onTranslateClick = onTranslateClick,
            onAuthorClick = onAuthorClick,
            onUploaderClick = onUploaderClick,
            onDownloadWholeComic = onDownloadWholeComic,
            onTagClick = onTagClick,
            navigationToEpisodeDownload = navigationToEpisodeDownload,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComicDetailLoadedContent(
    state: UnitedDetailsUiState.Content,
    episodes: LazyPagingItems<Chapter>,
    downloadTasks: List<DownloadTask>,
    chapterProgress: List<ChapterProgressEntity>,
    previewTitle: String,
    coverModifier: Modifier,
    titleModifier: Modifier,
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
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val detail = state.detail
    val isDownloaded = remember(downloadTasks, detail.epsCount) {
        isComicFullyDownloaded(downloadTasks, detail.epsCount)
    }
    val lastReadChapter = remember(chapterProgress) {
        chapterProgress.maxByOrNull { it.lastReadAt }
    }
    val readOrder = lastReadChapter?.chapterId ?: 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = previewTitle,
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
        bottomBar = {
            ComicDetailBottomAppBar(
                isFavourited = detail.isFavourited,
                isLiked = detail.isLiked,
                isDownloaded = isDownloaded,
                hasReadProgress = lastReadChapter != null,
                onRead = { navigationToReader(detail.id, readOrder) },
                onToggleFavourite = {
                    dispatch(UnitedDetailsAction.ToggleFavorite)
                },
                onToggleLike = {
                    dispatch(UnitedDetailsAction.ToggleLike)
                },
                onDownloadWholeComic = {
                    onDownloadWholeComic(
                        detail.title,
                        detail.cover,
                        detail.epsCount,
                    )
                },
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
                        recommendations = state.recommendations,
                        coverModifier = coverModifier,
                        titleModifier = titleModifier,
                        onRecommendedComicClick = onRecommendedComicClick,
                        onTranslateClick = onTranslateClick,
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

// TODO: 这部分空间要均分，出了开始阅读 
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComicDetailBottomAppBar(
    isFavourited: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    hasReadProgress: Boolean,
    onRead: () -> Unit,
    onToggleFavourite: () -> Unit,
    onToggleLike: () -> Unit,
    onDownloadWholeComic: () -> Unit,
) {
    BottomAppBar(
        actions = {
            TooltipIconButton(
                tooltipText = if (isFavourited) "取消收藏" else "收藏漫画",
                icon = if (isFavourited) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (isFavourited) "取消收藏" else "收藏漫画",
                tint = if (isFavourited) MaterialTheme.colorScheme.primary
                else LocalContentColor.current,
                onClick = onToggleFavourite,
            )
            TooltipIconButton(
                tooltipText = if (isLiked) "取消点赞" else "点赞",
                icon = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = if (isLiked) "取消点赞" else "点赞",
                tint = if (isLiked) MaterialTheme.colorScheme.tertiary
                else LocalContentColor.current,
                onClick = onToggleLike,
            )
            TooltipIconButton(
                tooltipText = if (isDownloaded) "漫画已下载，点击重新下载" else "下载整本漫画",
                icon = Icons.Filled.Download,
                contentDescription = if (isDownloaded) "重新下载" else "下载整本漫画",
                tint = if (isDownloaded) MaterialTheme.colorScheme.secondary
                else LocalContentColor.current,
                onClick = onDownloadWholeComic,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRead,
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(if (hasReadProgress) "继续阅读" else "开始阅读")
                },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipIconButton(
    tooltipText: String,
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
        ),
        tooltip = {
            PlainTooltip {
                Text(tooltipText)
            }
        },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
            )
        }
    }
}

@Composable
private fun ComicDetailBottomAppBarSkeleton(shimmer: Shimmer) {
    BottomAppBar {
        repeat(3) {
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
            )
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.weight(1f))
        SkeletonBlock(
            shimmer = shimmer,
            modifier = Modifier
                .width(132.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@Composable
private fun ComicDetailInitializeContent(
    coverUrl: String,
    title: String,
    coverModifier: Modifier,
    titleModifier: Modifier,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val shimmer = rememberShimmer(
        shimmerBounds = ShimmerBounds.View,
    )

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
        bottomBar = {
            ComicDetailBottomAppBarSkeleton(shimmer = shimmer)
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            // 与真实页面保持相同的 Tab 行和内容层级，避免数据返回后整体跳动。
            PrimaryTabRow(selectedTabIndex = 0) {
                PageTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == 0,
                        onClick = {},
                        text = { Text(text = tab.title) },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                ComicDetailHeroSkeleton(
                    coverUrl = coverUrl,
                    title = title,
                    coverModifier = coverModifier,
                    titleModifier = titleModifier,
                    shimmer = shimmer,
                )
                ComicDetailBodySkeleton(shimmer = shimmer)
            }
        }
    }
}

@Composable
private fun ComicDetailHeroSkeleton(
    coverUrl: String,
    title: String,
    coverModifier: Modifier,
    titleModifier: Modifier,
    shimmer: Shimmer,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(318.dp),
    ) {
        if (coverUrl.isNotBlank()) {
            RetryableAsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.42f),
            )
        } else {
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (coverUrl.isNotBlank()) {
                RetryableAsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = coverModifier
                        .width(122.dp)
                        .height(166.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            } else {
                SkeletonBlock(
                    shimmer = shimmer,
                    modifier = coverModifier
                        .width(122.dp)
                        .height(166.dp),
                    shape = RoundedCornerShape(14.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBlock(
                    shimmer = shimmer,
                    modifier = Modifier
                        .width(58.dp)
                        .height(26.dp),
                    shape = RoundedCornerShape(50),
                )
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = titleModifier,
                    )
                } else {
                    SkeletonBlock(
                        shimmer = shimmer,
                        modifier = titleModifier
                            .fillMaxWidth()
                            .height(28.dp),
                    )
                    SkeletonBlock(
                        shimmer = shimmer,
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .height(28.dp),
                    )
                }
                SkeletonBlock(
                    shimmer = shimmer,
                    modifier = Modifier
                        .fillMaxWidth(0.76f)
                        .height(14.dp),
                )
                SkeletonBlock(
                    shimmer = shimmer,
                    modifier = Modifier
                        .fillMaxWidth(0.52f)
                        .height(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ComicDetailBodySkeleton(shimmer: Shimmer) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SkeletonCard(height = 76.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(3) {
                    SkeletonBlock(
                        shimmer = shimmer,
                        modifier = Modifier
                            .width(72.dp)
                            .height(34.dp),
                    )
                }
            }
        }

        SkeletonCard(height = 132.dp) {
            SkeletonBlock(
                shimmer = shimmer, modifier = Modifier
                    .width(54.dp)
                    .height(20.dp))
            SkeletonBlock(
                shimmer = shimmer, modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp))
            SkeletonBlock(
                shimmer = shimmer, modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp))
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(14.dp)
            )
        }

        SkeletonCard(height = 112.dp) {
            SkeletonBlock(
                shimmer = shimmer, modifier = Modifier
                    .width(54.dp)
                    .height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(64.dp, 82.dp, 58.dp).forEach { width ->
                    SkeletonBlock(
                        shimmer = shimmer,
                        modifier = Modifier
                            .width(width)
                            .height(30.dp),
                        shape = RoundedCornerShape(50),
                    )
                }
            }
        }

        SkeletonCard(height = 142.dp) {
            SkeletonBlock(
                shimmer = shimmer, modifier = Modifier
                    .width(72.dp)
                    .height(20.dp))
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SkeletonBlock(
                        shimmer = shimmer,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    )
                    SkeletonBlock(
                        shimmer = shimmer,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonCard(
    height: androidx.compose.ui.unit.Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun SkeletonBlock(
    shimmer: Shimmer,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(7.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            // shimmer 必须位于 background 之前，才能作用于随后绘制的占位色。
            .shimmer(shimmer)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}
