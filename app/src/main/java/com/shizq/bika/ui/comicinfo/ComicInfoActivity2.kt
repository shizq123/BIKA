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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.ui.collapsingtoolbar.CollapsingTopBar
import com.shizq.bika.ui.reader.ReaderActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComicInfoActivity2 : ComponentActivity() {
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

    @Composable
    fun ComicDetailScreen(viewModel: ComicInfoViewModel2 = hiltViewModel()) {
        val comicDetailUiState by viewModel.comicDetailUiState.collectAsStateWithLifecycle()
        val episodes = viewModel.episodesFlow.collectAsLazyPagingItems()
        val relatedComicsUiState by viewModel.recommendationsUiState.collectAsStateWithLifecycle()
        ComicDetailContent(
            comicDetailState = comicDetailUiState,
            relatedComicsState = relatedComicsUiState,
            episodes = episodes,
            onBackClick = ::finish,
            onFavoriteClick = {},
            onLikeClick = {},
            onContinueReading = { id, index ->
                ReaderActivity.start(this, id, index)
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ComicDetailContent(
        comicDetailState: ComicDetailUiState,
        episodes: LazyPagingItems<Episode>,
        onBackClick: () -> Unit = {},
        onFavoriteClick: () -> Unit = {},
        onLikeClick: () -> Unit = {},
        onContinueReading: (String, Int) -> Unit = { _, _ -> },
        relatedComicsState: RecommendationsUiState,
    ) {
        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CollapsingTopBar(
                    title = (comicDetailState as? ComicDetailUiState.Success)?.detail?.title ?: "",
                    imageModel = (comicDetailState as? ComicDetailUiState.Success)?.detail?.cover,
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
                        (comicDetailState as? ComicDetailUiState.Success)?.detail?.id?.let {
                            onContinueReading(it, 1)
                        }
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    when (comicDetailState) {
                        is ComicDetailUiState.Error -> {

                        }

                        ComicDetailUiState.Loading -> {

                        }

                        is ComicDetailUiState.Success -> {
                            val detail = comicDetailState.detail
                            ComicInfoPanel(
                                title = detail.title,
                                tags = detail.tags,
                                description = detail.description,
                                author = detail.author,
                            )
                        }
                    }
                }
                item {
                    Text(
                        "章节列表",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(
                    count = episodes.itemCount,
                    key = episodes.itemKey { it.id }
                ) { index ->
                    episodes[index]?.let { episode ->
                        EpisodeItem(
                            episode = episode,
                            onClick = {
                                (comicDetailState as? ComicDetailUiState.Success)?.detail?.id?.let {
                                    onContinueReading(it, episode.order)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item {
                    RelatedComicsSection(relatedComicsState)
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    /**
     * 漫画详情信息面板
     *
     * @param title 标题
     * @param tags 标签列表
     * @param description 简介
     */
    @Composable
    fun ComicInfoPanel(
        author: String,
        title: String,
        tags: List<String>,
        description: String,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            ComicSynopsis(
                description = description,
                author = author,
            )

            TagsRow(tags = tags)
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
        description: String,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "作者: $author",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.typography.bodyMedium.color,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "简介: $description",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun EpisodeItem(
        episode: Episode,
        onClick: (Episode) -> Unit,
        modifier: Modifier = Modifier
    ) {
        OutlinedButton(
            onClick = { onClick(episode) },
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = episode.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun RelatedComicsSection(state: RecommendationsUiState) {
        when (state) {
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
                        items(state.comics, key = { it.id }) { summary ->
                            ComicCoverItem(
                                imageUrl = summary.coverUrl,
                                title = summary.title,
                                modifier = Modifier.width(120.dp)
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
        EpisodeItem(
            episode = Episode(
                id = "1",
                title = "第一话",
                order = 1,
                updatedAt = ""
            ),
            onClick = {}
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun ComicSynopsisPreview() {
        MaterialTheme {
            ComicSynopsis(
                author = "作者",
                description = "这是一段很长很长的简介，长到可以换行。这是一段很长很长的简介，长到可以换行。这是一段很长很长的简介，长到可以换行。这是一段很长很长的简介，长到可以换行。"
            )
        }
    }

    companion object {
        fun start(context: Context, id: String) {
            val intent = Intent(context, ComicInfoActivity2::class.java)
            intent.putExtra("id", id)
            context.startActivity(intent)
        }
    }
}