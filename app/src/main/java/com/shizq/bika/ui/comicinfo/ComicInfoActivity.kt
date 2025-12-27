package com.shizq.bika.ui.comicinfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.ui.comicinfo.page.ComicDetailPage
import com.shizq.bika.ui.comicinfo.page.CommentsPage
import com.shizq.bika.ui.comicinfo.page.EpisodeItem
import com.shizq.bika.ui.comicinfo.page.EpisodesPage
import com.shizq.bika.ui.comicinfo.page.PageTab
import com.shizq.bika.ui.comiclist.ComicListActivity
import com.shizq.bika.ui.comment.CommentsActivity
import com.shizq.bika.ui.reader.ReaderActivity
import com.shizq.bika.ui.theme.BikaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ComicInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            BikaTheme {
                ComicDetailScreen()
            }
        }
    }

    //
//        fun Creator() {
//            //搜索上传者
//            val intent = Intent(this@ComicInfoActivity, ComicListActivity::class.java)
//            intent.putExtra("tag", "knight")
//            intent.putExtra("title", binding.comicinfoCreatorName.text.toString())
//            intent.putExtra("value", viewModel.creatorId)
//            startActivity(intent)
//        }
    @Composable
    fun ComicDetailScreen(viewModel: ComicInfoViewModel = hiltViewModel()) {
        val state by viewModel.state.collectAsStateWithLifecycle()
//        val comicDetailUiState by viewModel.comicDetailUiState.collectAsStateWithLifecycle()
        val episodes = viewModel.episodesFlow.collectAsLazyPagingItems()
//        val relatedComicsUiState by viewModel.recommendationsUiState.collectAsStateWithLifecycle()

        val pinnedComments by viewModel.pinnedComments.collectAsStateWithLifecycle()
        val regularComments = viewModel.regularComments.collectAsLazyPagingItems()

        ComicDetailContent(
            unitedState = state,
//            relatedComicsState = relatedComicsUiState,
            episodes = episodes,
            onBackClick = ::finish,
            onContinueReading = { id, index ->
                ReaderActivity.start(this, id, index)
            },
            onTagClick = { tag ->
                ComicListActivity.start(this, "tags", tag, tag)
            },
            onAuthorClick = { authorName ->
                val intent = Intent(this, ComicListActivity::class.java).apply {
                    putExtra("tag", "author")
                    putExtra("title", authorName)
                    putExtra("value", authorName)
                }
                startActivity(intent)
            },
            onTranslatorClick = { translatorName ->
                val intent = Intent(this, ComicListActivity::class.java).apply {
                    putExtra("tag", "translate")
                    putExtra("title", translatorName)
                    putExtra("value", translatorName)
                }
                startActivity(intent)
            },
//            onCreatorClick = {  creatorName ->
//                val intent = Intent(this, ComicListActivity::class.java).apply {
//                    putExtra("tag", "knight")
//                    putExtra("title", creatorName)
//                    putExtra("value", creatorId)
//                }
//                startActivity(intent)
//            },
            onCommentClick = { comicId ->
                val intent = Intent(this, CommentsActivity::class.java).apply {
                    putExtra("id", comicId)
                    putExtra("comics_games", "comics")
                }
                startActivity(intent)
            },
            navigationToComicInfo = {
                start(this, it)
            },
            pinnedComments = pinnedComments,
            regularComments = regularComments,
            onToggleCommentLike = viewModel::toggleCommentLike,
            dispatch = viewModel::dispatch
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ComicDetailContent(
        unitedState: UnitedDetailsUiState,
//        relatedComicsState: RecommendationsUiState,
        episodes: LazyPagingItems<Episode>,
        pinnedComments: List<Comment>,
        regularComments: LazyPagingItems<Comment>,
        onBackClick: () -> Unit = {},
        onContinueReading: (String, Int) -> Unit = { _, _ -> },
        onTagClick: (String) -> Unit = {},
        onAuthorClick: (String) -> Unit = {},
        onTranslatorClick: (String) -> Unit = {},
        onCommentClick: (String) -> Unit = {},
        navigationToComicInfo: (String) -> Unit = {},
        onToggleCommentLike: (String) -> Unit = {},
        dispatch: (UnitedDetailsAction) -> Unit = {},
    ) {
        when (unitedState) {
            is UnitedDetailsUiState.Error -> {}
            UnitedDetailsUiState.I -> {}

            is UnitedDetailsUiState.Content -> {
                if (unitedState.detail == null) return

                val detail = unitedState.detail

                val topAppBarState = rememberTopAppBarState()

                val scrollBehavior =
                    TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = topAppBarState)

                Scaffold(
                    topBar = {
                    },
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                    ) {
                        val pagerState = rememberPagerState { PageTab.entries.size }
                        val scope = rememberCoroutineScope()

                        PrimaryTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(MaterialTheme.colorScheme.surface),
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
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Top,
                            key = { it }
                        ) { page ->
                            when (page) {
                                0 -> ComicDetailPage(
                                    detail = detail,
                                    recommendations = unitedState.recommendations
                                )

                                1 -> EpisodesPage(
                                    episodes = episodes,
                                    navigateToReader = {
                                        detail.id to it
                                    }
                                )

                                2 -> CommentsPage(
                                    pinnedComments = pinnedComments,
                                    regularComments = regularComments,
                                    onToggleCommentLike = onToggleCommentLike,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBarOverlay(
        scrollBehavior: TopAppBarScrollBehavior,
        title: String,
        imageUrl: String,
        expandedHeight: Dp,
        modifier: Modifier = Modifier,
        onBackClick: () -> Unit = {},
        navigationIcon: @Composable () -> Unit = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions: @Composable RowScope.() -> Unit = {},
    ) {
        val collapsedFraction = scrollBehavior.state.collapsedFraction

        val contentColor = lerp(
            start = Color.White,
            stop = MaterialTheme.colorScheme.onSurface,
            fraction = collapsedFraction
        )

        Box(modifier = modifier) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(expandedHeight)
                    .graphicsLayer {
                        translationY = scrollBehavior.state.heightOffset / 2
                        alpha = 1f - collapsedFraction
                    }
            )
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer {
                            alpha = collapsedFraction
                        }
                    )
                },
                navigationIcon = navigationIcon,
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = contentColor,
                    actionIconContentColor = contentColor,
                    titleContentColor = contentColor
                ),
                scrollBehavior = scrollBehavior
            )
        }
    }

    @Composable
    fun RelatedComicsSection(
//        recommendationsState: RecommendationsUiState,
//        navigationToComicInfo: (String) -> Unit = {}
    ) {
//        when (recommendationsState) {
//            is RecommendationsUiState.Error -> Text("推荐加载失败", Modifier.padding(16.dp))
//            RecommendationsUiState.Loading -> CircularProgressIndicator(Modifier.padding(16.dp))
//            is RecommendationsUiState.Success -> {
//                Column {
//                    Text(
//                        "相关推荐",
//                        style = MaterialTheme.typography.titleLarge,
//                        modifier = Modifier.padding(16.dp)
//                    )
//                    LazyRow(
//                        horizontalArrangement = Arrangement.spacedBy(12.dp),
//                        contentPadding = PaddingValues(horizontal = 16.dp)
//                    ) {
//                        items(recommendationsState.comics, key = { it.id }) { summary ->
//                            ComicCoverItem(
//                                imageUrl = summary.coverUrl,
//                                title = summary.title,
//                                modifier = Modifier
//                                    .width(120.dp)
//                                    .clickable(
//                                        interactionSource = remember { MutableInteractionSource() },
//                                        indication = null
//                                    ) { navigationToComicInfo(summary.id) }
//                            )
//                        }
//                    }
//                }
//            }
//        }
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

    companion object {
        fun start(context: Context, id: String) {
            val intent = Intent(context, ComicInfoActivity::class.java)
            intent.putExtra("id", id)
            context.startActivity(intent)
        }
    }
}