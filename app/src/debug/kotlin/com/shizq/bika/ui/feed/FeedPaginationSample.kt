@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.shizq.bika.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.filter
import com.shizq.bika.core.designsystem.theme.BikaTheme
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.RemoteImage
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.paging.FixedPagePagingSource
import com.shizq.bika.paging.PageInfoReporting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun FeedPaginationSample() {
    val controller = remember { FeedPagingController(supportsPagination = true) }
    val paging by controller.state.collectAsState()
    var continuous by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(SortOrder.NEWEST) }
    val hidden = remember { MutableStateFlow(false) }
    val hideItems by hidden.collectAsState()
    val requests = remember { mutableStateListOf<Int>() }
    LaunchedEffect(continuous, sort) { controller.configure(sort, continuous) }

    BikaTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
            Column(Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("连续追加", modifier = Modifier.weight(1f))
                    Switch(checked = continuous, onCheckedChange = { continuous = it })
                    TextButton(onClick = { hidden.value = !hideItems }) {
                        Text(if (hideItems) "显示内容" else "空页测试")
                    }
                }
                Text(
                    text = "请求页：${requests.joinToString()} · 共 ${requests.size} 次",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                paging.request?.let { request ->
                    key(request.revision) {
                        val scope = rememberCoroutineScope()
                        val comicsFlow = remember(request) {
                            val pages = Pager(
                                PagingConfig(pageSize = 5, initialLoadSize = 5, prefetchDistance = 1),
                            ) {
                                val source = SamplePagingSource { page ->
                                    withContext(Dispatchers.Main) { requests += page }
                                }
                                controller.track(
                                    request,
                                    if (request.continuousScrollEnabled) source
                                    else FixedPagePagingSource(source, request.page),
                                )
                            }.flow.cachedIn(scope)
                            combine(pages, hidden) { data, hide ->
                                if (hide) data.filter { false } else data
                            }.cachedIn(scope)
                        }
                        val comics = comicsFlow.collectAsLazyPagingItems()
                        SharedTransitionLayout(modifier = Modifier.weight(1f)) {
                            val sharedScope = this
                            AnimatedVisibility(visible = true) {
                                FeedScreen(
                                    title = "分页验证",
                                    state = FeedUiState(
                                        data = FeedData(query = FeedQuery(sort = sort)),
                                        preferencesReady = true,
                                        pagination = FeedPagination(
                                            continuousScrollEnabled = request.continuousScrollEnabled,
                                            currentPage = request.page,
                                            totalPages = paging.totalPages,
                                            revision = request.revision,
                                        ),
                                    ),
                                    comics = comics,
                                    currentTag = null,
                                    onAction = { action ->
                                        when (action) {
                                            FeedAction.NextPage -> controller.changePage(request.page + 1)
                                            FeedAction.PreviousPage -> controller.changePage(request.page - 1)
                                            is FeedAction.JumpToPage -> controller.changePage(action.page)
                                            is FeedAction.ChangeSort -> sort = action.sort
                                            else -> Unit
                                        }
                                    },
                                    sharedTransitionScope = sharedScope,
                                    animatedVisibilityScope = this,
                                    onNavigate = {},
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private class SamplePagingSource(
    private val onRequest: suspend (Int) -> Unit,
) : PagingSource<Int, ComicSummary>(), PageInfoReporting {
    override var onPageInfoLoaded: ((Int, Int) -> Unit)? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ComicSummary> {
        val page = params.key ?: 1
        onRequest(page)
        onPageInfoLoaded?.invoke(3, 15)
        return LoadResult.Page(
            data = (1..5).map { item ->
                ComicSummary(
                    id = "sample-$page-$item",
                    title = "第${page}页 · 示例作品$item",
                    author = "星野工作室",
                    categories = listOf("科幻"),
                    epsCount = item,
                    pagesCount = item * 20,
                    image = RemoteImage(fileServer = "file:///android_asset", path = "feed_sample.png"),
                )
            },
            prevKey = null,
            nextKey = (page + 1).takeIf { it <= 3 },
        )
    }

    override fun getRefreshKey(state: PagingState<Int, ComicSummary>): Int? = null
}
