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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.core.model.ComicSimple
import com.shizq.bika.core.ui.ComicCard

@Composable
fun FeedScreen(
    onBackClick: () -> Unit,
    navigationToComicDetail: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
    title: String
) {
    val items = viewModel.pagedComics.collectAsLazyPagingItems()

    FeedContent(
        title = title,
        items = items,
        onBackClick = onBackClick,
        navigationToComicDetail = navigationToComicDetail,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedContent(
    title: String,
    items: LazyPagingItems<ComicSimple>,
    navigationToComicDetail: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            FeedAppBar(
                topicLabel = title,
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {
            items(items.itemCount, key = items.itemKey { it.id }) { index ->
                items[index]?.let { item ->
                    ComicCard(comic = item) {
                        navigationToComicDetail(item.id)
                    }
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