package com.shizq.bika.ui.comicinfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.ui.comicinfo.page.ComicDetailPage
import com.shizq.bika.ui.comicinfo.page.CommentsPage
import com.shizq.bika.ui.comicinfo.page.EpisodeItem
import com.shizq.bika.ui.comicinfo.page.EpisodesPage
import com.shizq.bika.ui.comicinfo.page.PageTab
import com.shizq.bika.ui.comiclist.ComicListActivity
import com.shizq.bika.ui.comment.CommentsActivity
import com.shizq.bika.ui.reader.ReaderActivity
import com.shizq.bika.ui.theme.BikaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ComicInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            BikaTheme {
                ComicDetailScreen()
            }
        }
    }

    //
//        fun Creator() {
//            //搜索上传者
//            val intent = Intent(this@ComicInfoActivity, ComicListActivity::class.java)
//            intent.putExtra("tag", "knight")
//            intent.putExtra("title", binding.comicinfoCreatorName.text.toString())
//            intent.putExtra("value", viewModel.creatorId)
//            startActivity(intent)
//        }
    @Composable
    fun ComicDetailScreen(viewModel: ComicInfoViewModel = hiltViewModel()) {
        val state by viewModel.state.collectAsStateWithLifecycle()
//        val comicDetailUiState by viewModel.comicDetailUiState.collectAsStateWithLifecycle()
        val episodes = viewModel.episodesFlow.collectAsLazyPagingItems()
//        val relatedComicsUiState by viewModel.recommendationsUiState.collectAsStateWithLifecycle()

        val pinnedComments by viewModel.pinnedComments.collectAsStateWithLifecycle()
        val regularComments = viewModel.regularComments.collectAsLazyPagingItems()

        val replyList = viewModel.replyList.collectAsLazyPagingItems()
        ComicDetailContent(
            unitedState = state,
//            relatedComicsState = relatedComicsUiState,
            episodes = episodes,
            onBackClick = ::finish,
            navigationToReader = { id, index ->
                ReaderActivity.start(this, id, index)
            },
            //            onCreatorClick = {  creatorName ->
//                val intent = Intent(this, ComicListActivity::class.java).apply {
//                    putExtra("tag", "knight")
//                    putExtra("title", creatorName)
//                    putExtra("value", creatorId)
//                }
//                startActivity(intent)
//            },
            onCommentClick = { comicId ->
                val intent = Intent(this, CommentsActivity::class.java).apply {
                    putExtra("id", comicId)
                    putExtra("comics_games", "comics")
                }
                startActivity(intent)
            },
            navigationToComicInfo = {
                start(this, it)
            },
            navigationToComicList = { type, value ->
                ComicListActivity.start(this, type, value, value)
            },
            pinnedComments = pinnedComments,
            regularComments = regularComments,
            onToggleCommentLike = viewModel::toggleCommentLike,
            dispatch = viewModel::dispatch,
            onNavigate = viewModel::setReplyId,
            replyList = replyList
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ComicDetailContent(
        unitedState: UnitedDetailsUiState,
        episodes: LazyPagingItems<Episode>,
        pinnedComments: List<Comment>,
        regularComments: LazyPagingItems<Comment>,
        onBackClick: () -> Unit = {},
        navigationToReader: (id: String, index: Int) -> Unit = { _, _ -> },
        onCommentClick: (String) -> Unit = {},
        navigationToComicInfo: (String) -> Unit = {},
        navigationToComicList: (type: String, value: String) -> Unit = { _, _ -> },
        onToggleCommentLike: (String) -> Unit = {},
        dispatch: (UnitedDetailsAction) -> Unit = {},
        onNavigate: (String) -> Unit = {},
        replyList: LazyPagingItems<Comment>,
    ) {
        when (unitedState) {
            is UnitedDetailsUiState.Error -> {}
            UnitedDetailsUiState.I -> {}

            is UnitedDetailsUiState.Content -> {
                if (unitedState.detail == null) return

                val detail = unitedState.detail

                Scaffold(
                    topBar = {
                    },
                    modifier = Modifier,
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                    ) {
                        val pagerState = rememberPagerState { PageTab.entries.size }
                        val scope = rememberCoroutineScope()

                        PrimaryTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(MaterialTheme.colorScheme.surface),
                        ) {
                            PageTab.entries.forEachIndexed { index, tab ->
                                Tab(
                                    selected = index == pagerState.currentPage,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                    text = { Text(text = tab.title) }
                                )
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Top,
                            key = { it }
                        ) { page ->
                            when (page) {
                                0 -> ComicDetailPage(
                                    detail = detail,
                                    recommendations = unitedState.recommendations,
                                    onFavoriteClick = { dispatch(UnitedDetailsAction.ToggleFavorite) },
                                    onLikedClick = { dispatch(UnitedDetailsAction.ToggleLike) },
                                    navigationToReader = { navigationToReader(detail.id, 1) },
                                    navigationToSearch = { type, value ->
                                        navigationToComicList(type, value)
                                    },
                                    navigationToComicInfo = { navigationToComicInfo(it) }
                                )

                                1 -> EpisodesPage(
                                    episodes = episodes,
                                    navigateToReader = {
                                        navigationToReader(detail.id, it)
                                    }
                                )

                                2 -> CommentsPage(
                                    pinnedComments = pinnedComments,
                                    regularComments = regularComments,
                                    onToggleCommentLike = onToggleCommentLike,
                                    replyList = replyList,
                                    onExpandReplies = {
                                        dispatch(
                                            UnitedDetailsAction.ExpandReplies(
                                                it
                                            )
                                        )
                                    },
//                                    onPostComment = { text, replyToId ->
//                                        // 将发送评论的意图 dispatch 出去
//                                        dispatch(UnitedDetailsAction.PostComment(text, replyToId))
//                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun EpisodeItemPreview() {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            modifier = Modifier.heightIn(max = 400.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(count = 30) { index ->
                EpisodeItem(
                    text = "第${index}话",
                    onClick = { },
                )
            }
        }
    }

    companion object {
        fun start(context: Context, id: String) {
            val intent = Intent(context, ComicInfoActivity::class.java)
            intent.putExtra("id", id)
            context.startActivity(intent)
        }
    }
}