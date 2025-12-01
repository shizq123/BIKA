package com.shizq.bika.ui.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.ui.comiclist.ComicListActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SearchScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SearchScreen(
        searchViewModel: SearchViewModel = hiltViewModel()
    ) {
        val recentSearchQueriesUiState by searchViewModel.recentSearchQueriesUiState.collectAsStateWithLifecycle()
        val searchQuery by searchViewModel.searchQuery.collectAsStateWithLifecycle()

        SearchContent(
            searchQuery = searchQuery,
            recentSearchesUiState = recentSearchQueriesUiState,
            onSearchQueryChanged = searchViewModel::onSearchQueryChanged,
            onSearchTriggered = {
                searchViewModel.onSearchTriggered(it)

                val intent = Intent(this@SearchActivity, ComicListActivity::class.java)
                intent.putExtra("tag", "search")
                intent.putExtra("title", it)
                intent.putExtra("value", it)
                startActivity(intent)
            },
            onClearRecentSearches = searchViewModel::clearRecentSearches,
            onBackClicked = ::finish
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SearchContent(
        searchQuery: String,
        recentSearchesUiState: RecentSearchQueriesUiState,
        onSearchQueryChanged: (String) -> Unit = {},
        onSearchTriggered: (String) -> Unit = {},
        onClearRecentSearches: () -> Unit = {},
        onBackClicked: () -> Unit = {},
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("搜索") },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { innerPadding ->
            when (recentSearchesUiState) {
                is RecentSearchQueriesUiState.Error -> {}
                RecentSearchQueriesUiState.Loading -> {}
                is RecentSearchQueriesUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        SearchTextField(
                            searchQuery = searchQuery,
                            onSearchQueryChanged = onSearchQueryChanged,
                            onSearchTriggered = onSearchTriggered,
                        )
                        // Search History Section
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "历史", style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = onClearRecentSearches) {
                                    Text("清空")
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                itemVerticalAlignment = Alignment.CenterVertically,
                            ) {
                                recentSearchesUiState.recentQueries.fastForEach { recentSearchQuery ->
                                    AssistChip(
                                        onClick = {
                                            onSearchQueryChanged(recentSearchQuery.query)
                                            onSearchTriggered(recentSearchQuery.query)
                                        },
                                        label = { Text(recentSearchQuery.query) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Popular Searches Section
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "大家都在搜",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                itemVerticalAlignment = Alignment.CenterVertically,
                            ) {
                                recentSearchesUiState.hotKeywords.fastForEach { tag ->
                                    AssistChip(
                                        onClick = {
                                            onSearchQueryChanged(tag)
                                            onSearchTriggered(tag)
                                        },
                                        label = { Text(tag) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SearchTextField(
        searchQuery: String,
        onSearchQueryChanged: (String) -> Unit,
        onSearchTriggered: (String) -> Unit,
    ) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        val onSearchExplicitlyTriggered = {
            keyboardController?.hide()
            onSearchTriggered(searchQuery)
        }

        TextField(
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onSearchQueryChanged("")
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "清除搜索文本",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            onValueChange = {
                if ("\n" !in it) onSearchQueryChanged(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .focusRequester(focusRequester)
                .onKeyEvent {
                    if (it.key == Key.Enter) {
                        if (searchQuery.isBlank()) return@onKeyEvent false
                        onSearchExplicitlyTriggered()
                        true
                    } else {
                        false
                    }
                }
                .testTag("searchTextField"),
            shape = RoundedCornerShape(32.dp),
            value = searchQuery,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (searchQuery.isBlank()) return@KeyboardActions
                    onSearchExplicitlyTriggered()
                },
            ),
            maxLines = 1,
            singleLine = true,
        )
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}