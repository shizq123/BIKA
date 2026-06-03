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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.launch

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
    filterSelections: Map<FilterGroup, List<String>>,
    onFilterChanged: (group: FilterGroup, value: String) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpInputText by remember { mutableStateOf("") }
    var jumpInputError by remember { mutableStateOf<String?>(null) }
    val totalCount = pagedComics.itemCount

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = {
                showJumpDialog = false
                jumpInputText = ""
                jumpInputError = null
            },
            title = { Text("跳转到指定位置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "当前共 $totalCount 本漫画，请输入序号（1 ~ $totalCount）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = jumpInputText,
                        onValueChange = {
                            jumpInputText = it
                            jumpInputError = null
                        },
                        label = { Text("序号") },
                        singleLine = true,
                        isError = jumpInputError != null,
                        supportingText = jumpInputError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val index = jumpInputText.trim().toIntOrNull()
                                when {
                                    index == null -> jumpInputError = "请输入有效数字"
                                    index < 1 -> jumpInputError = "序号不能小于 1"
                                    index > totalCount -> jumpInputError = "序号不能超过 $totalCount"
                                    else -> {
                                        scope.launch { listState.animateScrollToItem(index - 1) }
                                        showJumpDialog = false
                                        jumpInputText = ""
                                        jumpInputError = null
                                    }
                                }
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val index = jumpInputText.trim().toIntOrNull()
                        when {
                            index == null -> jumpInputError = "请输入有效数字"
                            index < 1 -> jumpInputError = "序号不能小于 1"
                            index > totalCount -> jumpInputError = "序号不能超过 $totalCount"
                            else -> {
                                scope.launch { listState.animateScrollToItem(index - 1) }
                                showJumpDialog = false
                                jumpInputText = ""
                                jumpInputError = null
                            }
                        }
                    }
                ) { Text("跳转") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showJumpDialog = false
                        jumpInputText = ""
                        jumpInputError = null
                    }
                ) { Text("取消") }
            }
        )
    }

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
                totalCount = totalCount,
                onCountChipClick = { showJumpDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when (pagedComics.loadState.refresh) {
                is LoadState.Loading -> {
                    LoadingState(Modifier.weight(1f))
                }

                is LoadState.Error -> {
                    ErrorState(
                        onRetry = pagedComics::retry,
                        modifier = Modifier.weight(1f)
                    )
                }

                is LoadState.NotLoading -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f)
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
    totalCount: Int,
    onCountChipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(filterState.chips) { chipState ->
            FilterChip(
                state = chipState,
                onSelectionChanged = { value ->
                    chipState.kind?.let { group ->
                        onFilterChanged(group, value)
                    }
                }
            )
        }

        item {
            SuggestionChip(
                onClick = onCountChipClick,
                label = {
                    Text(
                        text = if (totalCount > 0) "$totalCount 本" else "加载中…",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (totalCount > 0)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (totalCount > 0)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderWidth = 0.dp,
                    borderColor = Color.Transparent
                )
            )
        }
    }
}