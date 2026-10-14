package com.shizq.bika.feature.comicdetail.impl

import androidx.compose.animation.core.LinearEasing

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.ui.ErrorState
import com.shizq.bika.feature.comicdetail.impl.comments.CommentsTab
import com.shizq.bika.feature.comicdetail.impl.detail.DetailTab
import com.shizq.bika.feature.comicdetail.impl.detail.PageTab
import com.shizq.bika.feature.comicdetail.impl.episodes.EpisodesRoute
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

private val LocalSkeletonBrush = compositionLocalOf<Brush> {
    error("Skeleton brush was not provided")
}

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
    viewModel: ComicInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val controls by viewModel.controls.collectAsStateWithLifecycle()
    val chapterProgress by viewModel.chapterProgress.collectAsStateWithLifecycle()

    ComicDetailContent(
        unitedState = state,
        controls = controls,
        episodesContent = {
            val detail = (state as? UnitedDetailsUiState.Content)?.detail
            if (detail != null) {
                EpisodesRoute(
                    episodesFlow = viewModel.episodesFlow,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComicDetailContent(
    unitedState: UnitedDetailsUiState,
    episodesContent: @Composable () -> Unit,
    controls: ComicDetailControlsState,
    navigationActions: ComicDetailNavigationActions,
    interactionActions: ComicDetailInteractionActions,
    downloadActions: ComicDetailDownloadActions,
    modifier: Modifier = Modifier,
) {
    when (unitedState) {
        UnitedDetailsUiState.Initialize -> ComicDetailInitializeContent(
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
    modifier: Modifier = Modifier,
    topBarTitle: @Composable () -> Unit = {},
    topBarActions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = topBarTitle,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回")
                    }
                },
                actions = topBarActions,

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
    actions: ComicDetailNavigationActions,
    interactions: ComicDetailInteractionActions,
    downloads: ComicDetailDownloadActions,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    val pagerState = rememberPagerState(pageCount = { PageTab.entries.size })
    val selectedTab = PageTab.entries[pagerState.currentPage]
    val coroutineScope = rememberCoroutineScope()
    val detailListState = rememberLazyListState()

    val tabStateHolder = rememberSaveableStateHolder()
    val density = LocalDensity.current

    val isDetailHeroExpanded by remember {
        derivedStateOf {
            detailListState.firstVisibleItemIndex == 0 &&
                    detailListState.firstVisibleItemScrollOffset < 24
        }
    }
    val toolbarAlpha by remember {
        derivedStateOf {
            if (selectedTab != PageTab.DETAIL) {
                1f
            } else {
                val revealDistancePx = with(density) { 180.dp.toPx() }
                val scrollOffset = if (detailListState.firstVisibleItemIndex == 0) {
                    detailListState.firstVisibleItemScrollOffset.toFloat()
                } else {
                    revealDistancePx
                }
                (scrollOffset / revealDistancePx).coerceIn(0f, 1f)
            }
        }
    }

    ComicDetailScaffold(
        onBackClick = actions.onBack,
        topBarTitle = {
            if (selectedTab != PageTab.DETAIL || !isDetailHeroExpanded) {
                Text(
                    text = detail.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(toolbarAlpha),
                )
            }
        },

        topBarActions = {
            when (selectedTab) {
                PageTab.DETAIL -> ComicQuickActions(
                    isFavourited = detail.isFavourited,
                    isLiked = detail.isLiked,
                    downloadState = controls.downloadState,
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

                PageTab.EPISODES -> ComicDownloadAction(
                    downloadState = controls.downloadState,
                    onDownloadWholeComic = {
                        downloads.onDownloadWholeComic(detail.title, detail.cover, detail.epsCount)
                    },
                )

                PageTab.COMMENT -> Unit
            }
        },
        bottomBar = {
            // 评论页保留自己的输入栏，目录页把空间完整交给章节列表。
            if (selectedTab == PageTab.DETAIL) {
                ComicReadBottomBar(
                    hasReadProgress = controls.hasReadProgress,
                    episodeCount = detail.epsCount,
                    onRead = {
                        actions.onRead(detail.id, controls.resumeChapterOrder)
                    },
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            key = { PageTab.entries[it] },
        ) { pageIndex ->
            when (val page = PageTab.entries[pageIndex]) {
                PageTab.DETAIL -> DetailTab(
                    detail = detail,
                    recommendations = state.recommendations,
                    onRecommendedComicClick = actions.onRecommendedComicClick,
                    onTranslateClick = actions.onTranslateClick,
                    navigationToTagBlock = interactions.onTagClick,
                    onAuthorClick = interactions.onAuthorClick,
                    onUploaderClick = interactions.onUploaderClick,
                    listState = detailListState,
                    tabs = {
                        ComicDetailTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            onTabClick = { targetPage ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            },
                        )
                    },
                )

                else -> Column(Modifier.fillMaxSize()) {
                    ComicDetailTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        onTabClick = { targetPage ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        },
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        when (page) {
                            PageTab.EPISODES -> episodesContent()
                            PageTab.COMMENT -> CommentsTab(comicId = detail.id)
                            PageTab.DETAIL -> Unit
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun ComicQuickActions(
    isFavourited: Boolean,
    isLiked: Boolean,
    downloadState: ComicDownloadState,
    onToggleFavourite: () -> Unit,
    onToggleLike: () -> Unit,
    onDownloadWholeComic: () -> Unit,
) {
    TooltipIconButton(
        tooltipText = if (isFavourited) "取消收藏" else "收藏漫画",
        icon = if (isFavourited) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
        contentDescription = if (isFavourited) "取消收藏" else "收藏漫画",
        tint = if (isFavourited) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        onClick = onToggleFavourite,
    )
    TooltipIconButton(
        tooltipText = if (isLiked) "取消点赞" else "点赞",
        icon = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
        contentDescription = if (isLiked) "取消点赞" else "点赞",
        tint = if (isLiked) MaterialTheme.colorScheme.tertiary else LocalContentColor.current,
        onClick = onToggleLike,
    )
    ComicDownloadAction(
        downloadState = downloadState,
        onDownloadWholeComic = onDownloadWholeComic,
    )
}

@Composable
private fun ComicDownloadAction(
    downloadState: ComicDownloadState,
    onDownloadWholeComic: () -> Unit,
) {
    TooltipIconButton(
        tooltipText = when (downloadState) {
            ComicDownloadState.NotDownloaded -> "下载整本漫画"
            is ComicDownloadState.Partial ->
                "继续下载（${downloadState.completedChapters}/${downloadState.expectedChapters}）"

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
}

@Composable
private fun ComicReadBottomBar(
    hasReadProgress: Boolean,
    episodeCount: Int,
    onRead: () -> Unit,
) {
    BottomAppBar(
        actions = {
            Text(
                text = if (hasReadProgress) "继续上次阅读" else "共 $episodeCount 话",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp),
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
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "comic-detail-skeleton")
    val shimmerOffset by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1_200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-offset",
    )
    val baseColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant
    val skeletonBrush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(shimmerOffset - 280f, 0f),
        end = Offset(shimmerOffset, 280f),
    )

    CompositionLocalProvider(LocalSkeletonBrush provides skeletonBrush) {
        ComicDetailScaffold(
            onBackClick = onBackClick,
            topBarTitle = {
                SkeletonBlock(
                    modifier = Modifier
                        .width(148.dp)
                        .height(20.dp),
                )
            },
            bottomBar = { /*ComicDetailBottomAppBarSkeleton()*/ },
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
                        ComicDetailHeroSkeleton()
                        ComicDetailBodySkeleton()
                    }
                },
            )
        }
    }
}

@Composable
private fun ComicDetailHeroSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp),
    ) {
        SkeletonBlock(
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(0.dp),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
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
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .width(122.dp)
                    .height(166.dp),
                shape = RoundedCornerShape(14.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .width(58.dp)
                        .height(30.dp),
                    shape = RoundedCornerShape(50),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(27.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(27.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .height(16.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.56f)
                        .height(16.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .width(54.dp)
                        .height(30.dp),
                    shape = RoundedCornerShape(50),
                )
            }
        }
    }
}

@Composable
private fun ComicReadBottomBarSkeleton() {
    BottomAppBar(
        actions = {
            SkeletonBlock(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .width(96.dp)
                    .height(16.dp),
            )
        },
        floatingActionButton = {
            SkeletonBlock(
                modifier = Modifier
                    .width(120.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            )
        },
    )
}

@Composable
private fun ComicDetailBodySkeleton() {
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkeletonBlock(
                            modifier = Modifier.size(18.dp),
                            shape = RoundedCornerShape(50),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SkeletonBlock(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(16.dp),
                            )
                            SkeletonBlock(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(11.dp),
                            )
                        }
                    }
                }
            }
        }

        SkeletonCard {
            SkeletonBlock(
                modifier = Modifier
                    .width(42.dp)
                    .height(20.dp),
            )
            repeat(2) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                )
            }
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(14.dp),
            )
        }

        SkeletonCard {
            SkeletonBlock(
                modifier = Modifier
                    .width(42.dp)
                    .height(20.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(64.dp, 82.dp, 58.dp).forEach { width ->
                    SkeletonBlock(
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
                modifier = Modifier
                    .width(82.dp)
                    .height(20.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(3) {
                    Column(
                        modifier = Modifier.width(112.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            shape = RoundedCornerShape(14.dp),
                        )
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp),
                        )
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(11.dp),
                        )
                    }
                }
            }
        }

        SkeletonCard {
            SkeletonBlock(
                modifier = Modifier
                    .width(72.dp)
                    .height(20.dp),
            )
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(2) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            SkeletonBlock(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(12.dp),
                            )
                            SkeletonBlock(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(18.dp),
                            )
                        }
                    }
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
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(7.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(LocalSkeletonBrush.current),
    )
}