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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.ui.collapsingtoolbar.CollapsingTopBar
import com.shizq.bika.ui.comicinfo.comment.CommentsScreen
import com.shizq.bika.ui.comiclist.ComicListActivity
import com.shizq.bika.ui.comment.CommentsActivity
import com.shizq.bika.ui.reader.ReaderActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
        val comicDetailUiState by viewModel.comicDetailUiState.collectAsStateWithLifecycle()
        val episodes = viewModel.episodesFlow.collectAsLazyPagingItems()
        val relatedComicsUiState by viewModel.recommendationsUiState.collectAsStateWithLifecycle()

        val pinnedComments by viewModel.pinnedComments.collectAsStateWithLifecycle()
        val regularComments = viewModel.regularComments.collectAsLazyPagingItems()

        LaunchedEffect(Unit) {
            snapshotFlow { comicDetailUiState }
                .filterIsInstance<ComicDetailUiState.Success>()
                .first()
                .let {
                    viewModel.recordVisit()
                }
        }
        ComicDetailContent(
            comicDetailState = comicDetailUiState,
            relatedComicsState = relatedComicsUiState,
            episodes = episodes,
            onBackClick = ::finish,
            onFavoriteClick = viewModel::toggleFavorite,
            onLikeClick = viewModel::toggleLike,
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
                ComicInfoActivity.start(this, it)
            },
            pinnedComments = pinnedComments,
            regularComments = regularComments,
            onToggleCommentLike = viewModel::toggleCommentLike
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ComicDetailContent(
        comicDetailState: ComicDetailUiState,
        relatedComicsState: RecommendationsUiState,
        episodes: LazyPagingItems<Episode>,
        pinnedComments: List<Comment>,
        regularComments: LazyPagingItems<Comment>,
        onBackClick: () -> Unit = {},
        onFavoriteClick: () -> Unit = {},
        onLikeClick: () -> Unit = {},
        onContinueReading: (String, Int) -> Unit = { _, _ -> },
        onTagClick: (String) -> Unit = {},
        onAuthorClick: (String) -> Unit = {},
        onTranslatorClick: (String) -> Unit = {},
        onCommentClick: (String) -> Unit = {},
        navigationToComicInfo: (String) -> Unit = {},
        onToggleCommentLike: (String) -> Unit,
    ) {
        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
        when (comicDetailState) {
            is ComicDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = comicDetailState.message)
                }
            }

            ComicDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ComicDetailUiState.Success -> {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        CollapsingTopBar(
                            title = comicDetailState.detail.title,
                            imageModel = comicDetailState.detail.cover,
                            scrollBehavior = scrollBehavior,
                            onBackClick = onBackClick
                        )
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            text = { Text("开始阅读") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onContinueReading(comicDetailState.detail.id, 1)
                            },
                        )
                    },
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        var selectedTabIndex by remember { mutableIntStateOf(0) }
                        val tabs = listOf("详情", "章节", "评论")
                        val pagerState = rememberPagerState { tabs.size }
                        val coroutineScope = rememberCoroutineScope()

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
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = { Text(text = title) }
                                )
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) { page ->
                            when (page) {
                                0 -> DetailPage(
                                    detail = comicDetailState.detail,
                                    episodes = episodes,
                                    relatedComicsState = relatedComicsState,
                                    onFavoriteClick = onFavoriteClick,
                                    onLikeClick = onLikeClick,
                                    onTagClick = onTagClick,
                                    onAuthorClick = onAuthorClick,
                                    onCommentClick = onCommentClick,
                                    onTranslatorClick = onTranslatorClick,
                                    onContinueReading = onContinueReading,
                                    navigationToComicInfo = navigationToComicInfo
                                )

                                1 -> Text("章节页")

                                2 -> CommentsScreen(
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
        episodes: LazyPagingItems<Episode>,
        relatedComicsState: RecommendationsUiState,
        modifier: Modifier = Modifier,
        onFavoriteClick: () -> Unit,
        onLikeClick: () -> Unit,
        onTagClick: (String) -> Unit,
        onAuthorClick: (String) -> Unit,
        onCommentClick: (String) -> Unit,
        onTranslatorClick: (String) -> Unit,
        onContinueReading: (String, Int) -> Unit,
        navigationToComicInfo: (String) -> Unit
    ) {
        // 使用 LazyColumn 替代 Column + verticalScroll，性能更优
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp), // 避免内容被FAB遮挡
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 漫画信息面板
            item {
                ComicInfoPanel(
                    detail,
                    onFavoriteClick = onFavoriteClick,
                    onLikeClick = onLikeClick,
                    onTagClick = onTagClick,
                    onAuthorClick = onAuthorClick,
                    onCommentClick = onCommentClick,
                    onTranslatorClick = onTranslatorClick,
                )
            }

            // 章节列表标题
            item {
                Text(
                    "章节列表",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 章节列表Grid
            item {
                // 注意：因为 LazyVerticalGrid 的高度是固定的，所以可以嵌套在 LazyColumn 的 item 中
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 96.dp),
                    modifier = Modifier
                        .heightIn(max = 400.dp) // 保持高度限制
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // 关键：由于父级是 LazyColumn，这里不需要独立的 verticalScroll
                    // 我们通过 heightIn 限制其大小，让 LazyColumn 负责整体滚动
                ) {
                    items(
                        count = episodes.itemCount,
                        key = episodes.itemKey { it.id }
                    ) { index ->
                        episodes[index]?.let { episode ->
                            EpisodeItem(
                                text = episode.title,
                                onClick = {
                                    onContinueReading(detail.id, episode.order)
                                },
                            )
                        }
                    }
                }
            }

            // 相关推荐
            item {
                RelatedComicsSection(
                    relatedComicsState,
                    navigationToComicInfo = navigationToComicInfo
                )
            }
        }
    }

    @Composable
    fun ComicInfoPanel(
        detail: ComicDetail,
        modifier: Modifier = Modifier,
        onFavoriteClick: () -> Unit = {},
        onLikeClick: () -> Unit = {},
        onTagClick: (String) -> Unit = {},
        onAuthorClick: (String) -> Unit = {},
        onCommentClick: (String) -> Unit = {},
        onTranslatorClick: (String) -> Unit = {},
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
                isFavorited = detail.isFavourite,
                isLiked = detail.isLiked,
                onFavoriteClick = onFavoriteClick,
                onLikeClick = onLikeClick,
                onAuthorClick = { onAuthorClick(detail.author) },
                onCommentClick = { onCommentClick(detail.id) },
                onTranslatorClick = { onTranslatorClick(detail.chineseTeam ?: "") },
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
        onFavoriteClick: () -> Unit,
        onLikeClick: () -> Unit,
        onAuthorClick: () -> Unit,
        onCommentClick: () -> Unit,
        onTranslatorClick: () -> Unit,
        modifier: Modifier = Modifier
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
                    onCheckedChange = { onFavoriteClick() }
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (isFavorited) MaterialTheme.colorScheme.error else LocalContentColor.current
                    )
                }
                IconToggleButton(
                    checked = isLiked,
                    onCheckedChange = { onLikeClick() }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "喜欢",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                IconButton(onCommentClick) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Comment,
                        contentDescription = "评论"
                    )
                }
            }
        }
    }

    @Composable
    fun EpisodeItem(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        TextButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun RelatedComicsSection(
        recommendationsState: RecommendationsUiState,
        navigationToComicInfo: (String) -> Unit = {}
    ) {
        when (recommendationsState) {
            is RecommendationsUiState.Error -> Text("推荐加载失败", Modifier.padding(16.dp))
            RecommendationsUiState.Loading -> CircularProgressIndicator(Modifier.padding(16.dp))
            is RecommendationsUiState.Success -> {
                Column {
                    Text(
                        "相关推荐",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(recommendationsState.comics, key = { it.id }) { summary ->
                            ComicCoverItem(
                                imageUrl = summary.coverUrl,
                                title = summary.title,
                                modifier = Modifier
                                    .width(120.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { navigationToComicInfo(summary.id) }
                            )
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
            onFavoriteClick = {},
            onLikeClick = {},
            onAuthorClick = {},
            modifier = Modifier,
            onCommentClick = {},
            chineseTeam = "哔咔汉化组",
            onTranslatorClick = {}
        )
    }
}