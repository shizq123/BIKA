package com.shizq.bika.ui.comiclist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.core.model.ComicSimple
import com.shizq.bika.core.ui.ComicCard
import com.shizq.bika.ui.tag.FilterChipsRow
import com.shizq.bika.ui.tag.FilterGroup
import com.shizq.bika.ui.tag.rememberFilterState

@Composable
internal fun TopicScreen(
    onBackClick: () -> Unit,
    navigationToComicInfo: (id: String) -> Unit,
    topicViewModel: TopicViewModel = hiltViewModel(),
) {
    val topic by topicViewModel.topicText.collectAsStateWithLifecycle()
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
        navigationToComicInfo = navigationToComicInfo,
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
    navigationToComicInfo: (String) -> Unit = {},
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
                    ComicCard(comic = comic) {
                        navigationToComicInfo(comic.id)
                    }
                }
            }
        }
    }
}