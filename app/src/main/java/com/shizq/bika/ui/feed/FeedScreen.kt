package com.shizq.bika.ui.feed

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.core.model.ComicSimple
import com.shizq.bika.core.ui.ComicCard

@Composable
fun FeedScreen(
    onBackClick: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val items = viewModel.pagedComics.collectAsLazyPagingItems()

    FeedContent(
        items = items,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedContent(
    items: LazyPagingItems<ComicSimple>,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            FeedAppBar(
                topicLabel = "Feed",
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {
            items(items.itemCount, key = items.itemKey { it.id }) { index ->
                items[index]?.let { item ->
                    ComicCard(comic = item)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedAppBar(
    topicLabel: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        modifier = modifier,
    )
}