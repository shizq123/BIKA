package com.shizq.bika.feature.comicdetail.impl

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.ui.ComicSharedElementKey
import com.shizq.bika.core.ui.ErrorState
import com.shizq.bika.core.ui.RetryableAsyncImage
import com.shizq.bika.feature.comicdetail.impl.comments.CommentsTab
import com.shizq.bika.feature.comicdetail.impl.detail.DetailTab
import com.shizq.bika.feature.comicdetail.impl.detail.PageTab
import com.shizq.bika.feature.comicdetail.impl.episodes.EpisodesRoute
import com.shizq.bika.feature.comicdetail.impl.episodes.EpisodesViewModel
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch

private data class ComicDetailNavigationActions(
    val onBack: () -> Unit,
    val onRead: (id: String, chapterOrder: Int) -> Unit,
    val onRecommendedComicClick: (String) -> Unit,
    val onTranslateClick: (String) -> Unit,
)

private data class ComicDetailInteractionActions(
    val dispatch: (UnitedDetailsAction) -> Unit,
    val onTagClick: (String) -> Unit,
    val onAuthorClick: (String) -> Unit,
    val onUploaderClick: (String, String) -> Unit,
)

private data class ComicDetailDownloadActions(
    val onDownloadWholeComic: (title: String, cover: String, epsCount: Int) -> Unit,
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ComicDetailScreen(
    navigationToReader: (id: String, chapterOrder: Int) -> Unit,
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
    viewModel: ComicInfoViewModel,
    episodesViewModel: EpisodesViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val controls by viewModel.controls.collectAsStateWithLifecycle()
    val chapterProgress by viewModel.chapterProgress.collectAsStateWithLifecycle()

    ComicDetailContent(
        unitedState = state,
        previewCoverUrl = previewCoverUrl,
        previewTitle = previewTitle,
        coverSharedElementKey = coverSharedElementKey,
        titleSharedElementKey = titleSharedElementKey,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        controls = controls,
        episodesContent = {
            val detail = (state as? UnitedDetailsUiState.Content)?.detail
            if (detail != null) {
                EpisodesRoute(
                    episodesFlow = episodesViewModel.episodesFlow,
                    chapterProgress = chapterProgress,
                    navigateToReader = { order -> navigationToReader(detail.id, order) },
                    onDownloadSelectionClick = {
                        navigationToEpisodeDownload(detail.id, detail.title, detail.cover)
                    },
                )
            }
        },
        navigationActions = ComicDetailNavigationActions(
            onBack = onBackClick,
            onRead = navigationToReader,
            onRecommendedComicClick = onForYouClick,
            onTranslateClick = navigationToFeed,
        ),
        interactionActions = ComicDetailInteractionActions(
            dispatch = viewModel::dispatch,
            onTagClick = onTagClick,
            onAuthorClick = onAuthorClick,
            onUploaderClick = onUploaderClick,
        ),
        downloadActions = ComicDetailDownloadActions(
            onDownloadWholeComic = { title, cover, epsCount ->
                viewModel.downloadWholeComic(title, cover, epsCount)
            },
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun ComicDetailContent(
    unitedState: UnitedDetailsUiState,
    episodesContent: @Composable () -> Unit,
    controls: ComicDetailControlsState,
    navigationActions: ComicDetailNavigationActions,
    interactionActions: ComicDetailInteractionActions,
    downloadActions: ComicDetailDownloadActions,
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
            onBackClick = navigationActions.onBack,
            modifier = modifier,
        )

        is UnitedDetailsUiState.Error -> ComicDetailScaffold(
            onBackClick = navigationActions.onBack,
            modifier = modifier,
        ) { innerPadding ->
            ErrorState(
                modifier = Modifier.padding(innerPadding),
                onRetry = {
                    interactionActions.dispatch(UnitedDetailsAction.Retry)
                },
            )
        }

        is UnitedDetailsUiState.Content -> ComicDetailLoadedContent(
            state = unitedState,
            episodesContent = episodesContent,
            controls = controls,
            previewTitle = previewTitle,
            coverModifier = sharedCoverModifier,
            titleModifier = sharedTitleModifier,
            actions = navigationActions,
            interactions = interactionActions,
            downloads = downloadActions,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComicDetailScaffold(
    onBackClick: () -> Unit,
    topBarTitle: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = topBarTitle,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = bottomBar,
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        content = content,
    )
}

@Composable
private fun ComicDetailTabRow(
    selectedTabIndex: Int,
    enabled: Boolean = true,
    onTabClick: (Int) -> Unit = {},
) {
    PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
        PageTab.entries.forEachIndexed { index, tab ->
            Tab(
                selected = index == selectedTabIndex,
                enabled = enabled,
                onClick = { onTabClick(index) },
                text = { Text(text = tab.title) },
            )
        }
    }
}

@Composable
private fun ComicDetailBodyLayout(
    innerPadding: PaddingValues,
    tabRow: @Composable () -> Unit,
    body: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize(),
    ) {
        tabRow()
        Box(
            modifier = Modifier.weight(1f),
        ) {
            body()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComicDetailLoadedContent(
    state: UnitedDetailsUiState.Content,
    episodesContent: @Composable () -> Unit,
    controls: ComicDetailControlsState,
    previewTitle: String,
    coverModifier: Modifier,
    titleModifier: Modifier,
    actions: ComicDetailNavigationActions,
    interactions: ComicDetailInteractionActions,
    downloads: ComicDetailDownloadActions,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    val pagerState = rememberPagerState { PageTab.entries.size }
    val scope = rememberCoroutineScope()

    ComicDetailScaffold(
        onBackClick = actions.onBack,
        topBarTitle = {
            Text(
                text = previewTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        bottomBar = {
            ComicDetailBottomAppBar(
                isFavourited = detail.isFavourited,
                isLiked = detail.isLiked,
                downloadState = controls.downloadState,
                hasReadProgress = controls.hasReadProgress,
                onRead = { actions.onRead(detail.id, controls.resumeChapterOrder) },
                onToggleFavourite = {
                    interactions.dispatch(UnitedDetailsAction.ToggleFavorite)
                },
                onToggleLike = {
                    interactions.dispatch(UnitedDetailsAction.ToggleLike)
                },
                onDownloadWholeComic = {
                    downloads.onDownloadWholeComic(detail.title, detail.cover, detail.epsCount)
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        ComicDetailBodyLayout(
            innerPadding = innerPadding,
            tabRow = {
                ComicDetailTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    onTabClick = { index ->
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                )
            },
            body = {
                ComicDetailPager(
                    pagerState = pagerState,
                    state = state,
                    episodesContent = episodesContent,
                    coverModifier = coverModifier,
                    titleModifier = titleModifier,
                    actions = actions,
                    interactions = interactions,
                )
            },
        )
    }
}

@Composable
private fun ComicDetailPager(
    pagerState: androidx.compose.foundation.pager.PagerState,
    state: UnitedDetailsUiState.Content,
    episodesContent: @Composable () -> Unit,
    coverModifier: Modifier,
    titleModifier: Modifier,
    actions: ComicDetailNavigationActions,
    interactions: ComicDetailInteractionActions,
) {
    val detail = state.detail
    var episodesVisited by rememberSaveable(detail.id) { mutableStateOf(false) }
    LaunchedEffect(pagerState.currentPage) {
        if (PageTab.entries[pagerState.currentPage] == PageTab.EPISODES) {
            episodesVisited = true
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Top,
        key = { it },
    ) { page ->
        when (PageTab.entries[page]) {
            PageTab.DETAIL -> DetailTab(
                detail = state.detail,
                recommendations = state.recommendations,
                coverModifier = coverModifier,
                titleModifier = titleModifier,
                onRecommendedComicClick = actions.onRecommendedComicClick,
                onTranslateClick = actions.onTranslateClick,
                navigationToTagBlock = interactions.onTagClick,
                onAuthorClick = interactions.onAuthorClick,
                onUploaderClick = interactions.onUploaderClick,
            )

            PageTab.EPISODES -> if (episodesVisited) episodesContent()

            PageTab.COMMENT -> CommentsTab(comicId = detail.id)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComicDetailBottomAppBar(
    isFavourited: Boolean,
    isLiked: Boolean,
    downloadState: ComicDownloadState,
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
                tooltipText = when (downloadState) {
                    ComicDownloadState.NotDownloaded -> "下载整本漫画"
                    is ComicDownloadState.Partial -> "继续下载（${downloadState.completedChapters}/${downloadState.expectedChapters}）"
                    is ComicDownloadState.Downloaded -> "漫画已下载，点击重新下载"
                },
                icon = Icons.Filled.Download,
                contentDescription = when (downloadState) {
                    ComicDownloadState.NotDownloaded -> "下载整本漫画"
                    is ComicDownloadState.Partial -> "继续下载漫画"
                    is ComicDownloadState.Downloaded -> "重新下载"
                },
                tint = when (downloadState) {
                    ComicDownloadState.NotDownloaded -> LocalContentColor.current
                    is ComicDownloadState.Partial -> MaterialTheme.colorScheme.primary
                    is ComicDownloadState.Downloaded -> MaterialTheme.colorScheme.secondary
                },
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color,
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
private fun ComicDetailInitializeContent(
    coverUrl: String,
    title: String,
    coverModifier: Modifier,
    titleModifier: Modifier,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shimmer = rememberShimmer(
        shimmerBounds = ShimmerBounds.View,
    )

    ComicDetailScaffold(
        onBackClick = onBackClick,
        bottomBar = {
            ComicDetailBottomAppBarSkeleton(shimmer = shimmer)
        },
        modifier = modifier,
    ) { innerPadding ->
        ComicDetailBodyLayout(
            innerPadding = innerPadding,
            tabRow = {
                ComicDetailTabRow(
                    selectedTabIndex = 0,
                    enabled = false,
                )
            },
            body = {
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
            },
        )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RetryableAsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = coverModifier
                .width(112.dp)
                .height(156.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                modifier = titleModifier,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(16.dp),
            )
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(16.dp),
            )
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(28.dp),
            )
        }
    }
}

@Composable
private fun ComicDetailBottomAppBarSkeleton(shimmer: Shimmer) {
    BottomAppBar(
        actions = {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                repeat(3) {
                    SkeletonBlock(
                        shimmer = shimmer,
                        modifier = Modifier
                            .width(32.dp)
                            .height(32.dp),
                        shape = RoundedCornerShape(50),
                    )
                }
            }
        },
        floatingActionButton = {
            SkeletonBlock(
                shimmer = shimmer,
                modifier = Modifier
                    .width(120.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            )
        },
    )
}

@Composable
private fun ComicDetailBodySkeleton(shimmer: Shimmer) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SkeletonCard {
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

        SkeletonCard {
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

        SkeletonCard {
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

        SkeletonCard {
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
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
    shape: Shape = RoundedCornerShape(7.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            // shimmer 必须位于 background 之前，才能作用于随后绘制的占位色。
            .shimmer(shimmer)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}
