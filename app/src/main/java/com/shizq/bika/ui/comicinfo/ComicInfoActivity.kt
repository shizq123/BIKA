package com.shizq.bika.ui.comicinfo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.flaringapp.compose.topbar.scaffold.CollapsingTopBarScaffold
import com.flaringapp.compose.topbar.scaffold.CollapsingTopBarScaffoldScrollMode
import com.flaringapp.compose.topbar.scaffold.rememberCollapsingTopBarScaffoldState
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.ui.comicinfo.page.ComicDetailPage
import com.shizq.bika.ui.comicinfo.page.CommentsPage
import com.shizq.bika.ui.comicinfo.page.EpisodeItem
import com.shizq.bika.ui.comicinfo.page.EpisodesPage
import com.shizq.bika.ui.comiclist.ComicListActivity
import com.shizq.bika.ui.comment.CommentsActivity
import com.shizq.bika.ui.reader.ReaderActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComicInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
        )
        super.onCreate(savedInstanceState)

        setContent {
            ComicDetailScreen()
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
        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
        when (unitedState) {
            is UnitedDetailsUiState.Error -> {}
            UnitedDetailsUiState.I -> {}

            is UnitedDetailsUiState.Content -> {
                if (unitedState.detail == null) return

                CollapsingTopBarScaffold(
                    scrollMode = CollapsingTopBarScaffoldScrollMode.collapse(false),
                    state = rememberCollapsingTopBarScaffoldState(),
                    topBar = {
                        var topBarColorProgress by remember { mutableFloatStateOf(1f) }
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(unitedState.detail.cover)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .progress { _, itemProgress ->
                                    topBarColorProgress =
                                        itemProgress.coerceAtMost(0.25f) / 0.25f
                                },
                        )

                        TopAppBar(
                            title = {
                                Text(text = unitedState.detail.title)
                            },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                containerColor = MaterialTheme.colorScheme.surface.copy(
                                    alpha = lerp(1f, 0f, topBarColorProgress),
                                ),
                            ),
                        )
                    },
                ) {
                    Column(
                        modifier = Modifier
//                            .padding(innerPadding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val tabs = listOf("详情", "章节", "评论")
                        var selectedTabIndex by remember { mutableIntStateOf(0) }
                        val pagerState = rememberPagerState { tabs.size }
                        LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                            if (!pagerState.isScrollInProgress) {
                                selectedTabIndex = pagerState.currentPage
                            }
                        }

                        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = index == selectedTabIndex,
                                    onClick = {
                                        selectedTabIndex = index
                                    },
                                    text = { Text(text = title) }
                                )
                            }
                        }
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .weight(1f)
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            verticalAlignment = Alignment.Top,
                        ) { page ->
                            when (page) {
                                0 -> ComicDetailPage(
                                    detail = unitedState.detail,
                                )

                                1 -> EpisodesPage(episodes = episodes)

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

    /**
     * 详情和章节页面
     */
    @Composable
    private fun DetailPage(
        detail: ComicDetail, // 使用具体类型
        //        relatedComicsState: RecommendationsUiState,
        modifier: Modifier = Modifier,
        onTagClick: (String) -> Unit,
        onAuthorClick: (String) -> Unit,
        onTranslatorClick: (String) -> Unit,
        navigationToComicInfo: (String) -> Unit,
        dispatch: (UnitedDetailsAction) -> Unit,
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ComicInfoPanel(
                    detail,
                    onTagClick = onTagClick,
                    onAuthorClick = onAuthorClick,
                    onTranslatorClick = onTranslatorClick,
                    dispatch = dispatch,
                )
            }

            item {
//                RelatedComicsSection(
//                    relatedComicsState,
//                    navigationToComicInfo = navigationToComicInfo
//                )
            }
        }
    }

    @Composable
    fun ComicInfoPanel(
        detail: ComicDetail,
        modifier: Modifier = Modifier,
        onTagClick: (String) -> Unit = {},
        onAuthorClick: (String) -> Unit = {},
        onTranslatorClick: (String) -> Unit = {},
        dispatch: (UnitedDetailsAction) -> Unit = {},
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = detail.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            ComicSynopsis(
                description = detail.description,
                author = detail.author,
                chineseTeam = detail.chineseTeam,
                isFavorited = detail.isFavourited,
                isLiked = detail.isLiked,
                onAuthorClick = { onAuthorClick(detail.author) },
                onTranslatorClick = { onTranslatorClick(detail.chineseTeam ?: "") },
                dispatch = dispatch,
            )

            TagsRow(tags = detail.tags, onTagClick = onTagClick)
        }
    }

    @Composable
    fun TagsRow(
        tags: List<String>,
        modifier: Modifier = Modifier,
        onTagClick: (String) -> Unit = {}
    ) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.fastForEach { tag ->
                OutlinedButton(
                    onClick = { onTagClick(tag) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    @Composable
    fun ComicSynopsis(
        author: String,
        chineseTeam: String?,
        description: String,
        isFavorited: Boolean,
        isLiked: Boolean,
        onAuthorClick: () -> Unit,
        onTranslatorClick: () -> Unit,
        modifier: Modifier = Modifier,
        dispatch: (UnitedDetailsAction) -> Unit = {},
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "简介: $description",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "作者: $author",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onAuthorClick() }
                )
                if (!chineseTeam.isNullOrEmpty()) {
                    Text(
                        text = "汉化组: $chineseTeam",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTranslatorClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Row {
                IconToggleButton(
                    checked = isFavorited,
                    onCheckedChange = { dispatch(UnitedDetailsAction.ToggleFavorite) }
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (isFavorited) MaterialTheme.colorScheme.error else LocalContentColor.current
                    )
                }
                IconToggleButton(
                    checked = isLiked,
                    onCheckedChange = { dispatch(UnitedDetailsAction.ToggleLike) }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "喜欢",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
            }
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

    @Preview(showBackground = true)
    @Composable
    fun ComicSynopsisPreview() {
        ComicSynopsis(
            author = "作者",
            description = "这是一段很长很长的简介，这是一段很长很长的简介，这是一段很长很长的简介，这是一段很长很长的简介。",
            isFavorited = false,
            isLiked = true,
            onAuthorClick = {},
            modifier = Modifier,
            chineseTeam = "哔咔汉化组",
            onTranslatorClick = {}
        )
    }
}