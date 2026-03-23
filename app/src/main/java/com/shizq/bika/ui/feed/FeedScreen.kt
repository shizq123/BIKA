package com.shizq.bika.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.core.model.ComicSimple
import com.shizq.bika.core.model.Sort
import com.shizq.bika.core.ui.ComicCard
import com.shizq.bika.core.ui.ErrorState
import com.shizq.bika.core.ui.LoadingState
import com.shizq.bika.ui.tag.FilterChip
import com.shizq.bika.ui.tag.FilterGroup
import com.shizq.bika.ui.tag.FilterState
import com.shizq.bika.ui.tag.rememberFilterState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onBackClick: () -> Unit,
    onComicClick: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
    title: String
) {
    val pagedComics = viewModel.pagedComics.collectAsLazyPagingItems()
    val currentSortOrder by viewModel.currentSortOrder.collectAsStateWithLifecycle()
    val filterSelections by viewModel.filterSelections.collectAsStateWithLifecycle()
    FeedContent(
        title = title,
        pagedComics = pagedComics,
        onBackClick = onBackClick,
        onComicClick = onComicClick,
        currentSortOrder = currentSortOrder,
        onSortOrderChanged = viewModel::updateSortOrder,
        filterSelections = filterSelections,
        onFilterChanged = viewModel::toggleFilter,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedAppBar(
    title: String,
    currentSortOrder: Sort,
    onSortOrderChanged: (Sort) -> Unit,
    onBackClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回"
                )
            }
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "排序"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                Sort.entries.fastForEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sort.title,
                                fontWeight = if (sort == currentSortOrder) FontWeight.Bold else FontWeight.Normal,
                                color = if (sort == currentSortOrder) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onSortOrderChanged(sort)
                        }
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FeedContent(
    title: String,
    pagedComics: LazyPagingItems<ComicSimple>,
    currentSortOrder: Sort,
    onSortOrderChanged: (Sort) -> Unit,
    onComicClick: (comicId: String) -> Unit,
    onBackClick: () -> Unit,
    // --- 新增过滤器相关参数 ---
    filterSelections: Map<FilterGroup, List<String>>,
    onFilterChanged: (group: FilterGroup, value: String) -> Unit, // 通知父组件去更新 Map
    // ----------------------
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
) {
    Scaffold(
        topBar = {
            FeedAppBar(
                title = title,
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick,
                currentSortOrder = currentSortOrder,
                onSortOrderChanged = onSortOrderChanged,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val filterState = rememberFilterState(filterSelections)

            FilterRow(
                filterState = filterState,
                onFilterChanged = onFilterChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            // 2. 根据分页加载状态显示内容
            when (pagedComics.loadState.refresh) {
                is LoadState.Loading -> {
                    LoadingState(Modifier.weight(1f)) // 使用 weight 占据剩余空间
                }

                is LoadState.Error -> {
                    ErrorState(
                        onRetry = pagedComics::retry,
                        modifier = Modifier.weight(1f)
                    )
                }

                is LoadState.NotLoading -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f) // 占据过滤器下方的所有剩余空间
                    ) {
                        items(pagedComics.itemCount, key = pagedComics.itemKey { it.id }) { index ->
                            pagedComics[index]?.let { item ->
                                ComicCard(comic = item) {
                                    onComicClick(item.id)
                                }
                            }
                        }

                        if (pagedComics.loadState.append is LoadState.Loading) {
                            item {
                                LoadingState(Modifier.wrapContentHeight())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    filterState: FilterState,
    onFilterChanged: (group: FilterGroup, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp) // Chip 之间的间距
    ) {
        items(filterState.chips) { chipState ->
            FilterChip(
                state = chipState,
                onSelectionChanged = { value ->
                    // 确保 kind 不为空时，将事件回传给外层
                    chipState.kind?.let { group ->
                        onFilterChanged(group, value)
                    }
                }
            )
        }
    }
}