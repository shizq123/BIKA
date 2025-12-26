package com.shizq.bika.ui.comicinfo.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.core.network.model.Episode
import kotlinx.coroutines.flow.flowOf

@Composable
fun EpisodesPage(
    episodes: LazyPagingItems<Episode>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            count = episodes.itemCount,
            key = episodes.itemKey { it.id }
        ) { index ->
            episodes[index]?.let { episode ->
                EpisodeItem(
                    text = episode.title,
                    onClick = {
//                        onContinueReading(detail.id, episode.order)
                    },
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

@Preview(showBackground = true, name = "Episodes Page Preview")
@Composable
private fun EpisodesPagePreview() {
    val fakeEpisodes = List(20) { i ->
        Episode(
            id = i.toString(),
            title = "第 ${i + 1} 话",
            order = i + 1,
            updatedAt = ""
        )
    }
    val pagingDataFlow = flowOf(PagingData.from(fakeEpisodes))
    val lazyPagingItems = pagingDataFlow.collectAsLazyPagingItems()

    MaterialTheme {
        EpisodesPage(episodes = lazyPagingItems)
    }
}

@Preview(showBackground = true, name = "Episode Item Preview")
@Composable
private fun EpisodeItemPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            EpisodeItem(
                text = "第 01 话",
                onClick = {}
            )
        }
    }
}