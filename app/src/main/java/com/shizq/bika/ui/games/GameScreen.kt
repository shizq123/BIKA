package com.shizq.bika.ui.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.shizq.bika.core.network.model.Game
import com.shizq.bika.paging.GameListPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    navigationToGameDetail: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    val gameList = viewModel.gameList.collectAsLazyPagingItems()
    GameContent(
        gameList,
        navigationToGameDetail = navigationToGameDetail,
        onBackClick = onBackClick
    )
}

@Composable
fun GameContent(
    gameList: LazyPagingItems<Game>,
    navigationToGameDetail: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {

        }
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(gameList.itemCount, key = gameList.itemKey { it.id }) { index ->
                gameList[index]?.let { item ->
                    GameItemCard(
                        game = item,
                        onClick = {
                            navigationToGameDetail(item.id)
                        }
                    )
                }
            }
        }
    }
}

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameListPagingSource: GameListPagingSource,
) : ViewModel() {
    val gameList = Pager(PagingConfig(100, initialLoadSize = 100)) {
        gameListPagingSource
    }.flow
        .cachedIn(viewModelScope)
}

@Composable
fun GameItemCard(
    game: Game,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = game.icon.originalImageUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.aspectRatio(1f)
            )

            Text(
                text = game.title,
                modifier = Modifier.padding(8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

