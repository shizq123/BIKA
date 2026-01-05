package com.shizq.bika.ui.comiclist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import coil3.compose.AsyncImage
import com.shizq.bika.core.network.model.ComicSimple
import com.shizq.bika.ui.tag.FilterChipsRow
import com.shizq.bika.ui.tag.FilterGroup
import com.shizq.bika.ui.tag.rememberFilterState

@Composable
internal fun TopicScreen(
    onBackClick: () -> Unit,
    topicViewModel: TopicViewModel = hiltViewModel(),
) {
    val topic by topicViewModel.topic.collectAsStateWithLifecycle()
    val pagedComics = topicViewModel.pagedComics.collectAsLazyPagingItems()
    val searchParameters by topicViewModel.searchParametersFlow.collectAsStateWithLifecycle()
    val availableTags by topicViewModel.availableTags.collectAsStateWithLifecycle()

    TopicContent(
        topicLabel = topic,
        pagedComics = pagedComics,
        selectedFilters = searchParameters.filters,
        availableTags = availableTags,
        updateFilters = topicViewModel::updateFilters,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicContent(
    topicLabel: String = "",
    pagedComics: LazyPagingItems<ComicSimple>,
    selectedFilters: Map<FilterGroup, List<String>> = emptyMap(),
    availableTags: List<String> = emptyList(),
    updateFilters: (Map<FilterGroup, List<String>>) -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topicLabel) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val filterState = rememberFilterState(selectedFilters, availableTags)

                FilterChipsRow(
                    state = filterState,
                    onSelectionChanged = { chipState, value ->
                        val kind = chipState.kind ?: return@FilterChipsRow

                        val currentSelection =
                            selectedFilters.getOrDefault(kind, emptyList()).toMutableList()

                        if (value in currentSelection) {
                            currentSelection.remove(value)
                        } else {
                            currentSelection.add(value)
                        }

                        val newFilters = selectedFilters.toMutableMap().apply {
                            this[kind] = currentSelection
                        }

                        updateFilters(newFilters)
                    }
                )
            }

            items(pagedComics.itemCount) {
                val comic = pagedComics[it]
                if (comic != null) {
                    ComicCard(comic = comic)
                }
            }
        }
    }
}

@Composable
fun ComicCard(
    comic: ComicSimple,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row {
            // 左侧：漫画封面
            Box {
                AsyncImage(
                    model = comic.image.originalImageUrl,
                    contentDescription = comic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                )
            }

            // 右侧：漫画信息
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 上半部分信息
                Column {
                    Text(
                        text = comic.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    InfoRow(
                        icon = {
                            Icon(
                                Icons.Default.PersonOutline,
                                contentDescription = "作者",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        text = comic.author
                    )
                }

                // 底部统计信息
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoRow(
                        icon = {
                            Icon(
                                Icons.Default.RemoveRedEye,
                                contentDescription = "阅读数",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        text = formatCount(comic.totalViews)
                    )
                    InfoRow(
                        icon = {
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = "喜欢数",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        text = formatCount(comic.totalLikes)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: @Composable () -> Unit, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 10000 -> {
            val thousands = count / 10000.0
            String.format("%.1f万", thousands)
        }

        else -> count.toString()
    }
}