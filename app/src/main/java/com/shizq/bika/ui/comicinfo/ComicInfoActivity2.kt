package com.shizq.bika.ui.comicinfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComicInfoActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ComicDetailScreen()
        }
    }

    @Composable
    fun ComicDetailScreen(viewModel: ComicInfoViewModel2 = hiltViewModel()) {
        val comicDetailUiState by viewModel.comicDetailUiState.collectAsStateWithLifecycle()
        ComicDetailContent(
            comicDetailUiState,
            onBackClick = ::finish
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ComicDetailContent(
        uiState: ComicDetailUiState,
        onBackClick: () -> Unit = {}
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: Menu */ }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "More")
                        }
                    },
                )
            },
        ) { innerPadding ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                when (uiState) {
                    ComicDetailUiState.Loading -> {

                    }

                    is ComicDetailUiState.Error -> {}

                    is ComicDetailUiState.Success -> {
                        val detail = uiState.detail
                        ComicDetailHeader(
                            coverImageUrl = detail.cover,
                            title = detail.title,
                            authorName = detail.author,
                            likeCount = detail.totalLikes,
                            isLiked = detail.isLiked,
                            isFavorite = detail.isFavourite,
                            commentCount = detail.commentsCount,
                            onLikeClick = {},
                            onFavoriteClick = { },
                            onCommentClick = {},
                            onShareClick = {}
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        ActionButtonsSection(onReadClick = {})
                        Spacer(modifier = Modifier.height(24.dp))

                        // 3. 信息区域 (标签流)
//                        InfoSection()

                        HorizontalDivider(thickness = 8.dp)
                    }
                }
            }
        }
    }

    @Composable
    fun ComicDetailHeader(
        coverImageUrl: String,
        title: String,
        authorName: String,
        likeCount: Int,
        isLiked: Boolean,
        isFavorite: Boolean,
        commentCount: Int,
        modifier: Modifier = Modifier,
        onLikeClick: () -> Unit = {},
        onFavoriteClick: () -> Unit = {},
        onCommentClick: () -> Unit = {},
        onShareClick: () -> Unit = {}
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ElevatedCard(
                    shape = MaterialTheme.shapes.small,
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .width(100.dp)
                        .aspectRatio(0.7f)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(coverImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover of $title",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = authorName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // 可以在这里预留位置放简单的分类标签，例如：
                    // Text(text = "同人 / 短篇", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ComicActionButton(
                    icon = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    label = formatCount(likeCount),
                    isActive = isLiked,
                    activeColor = MaterialTheme.colorScheme.error,
                    onClick = onLikeClick
                )

                ComicActionButton(
                    icon = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = if (isFavorite) "已收藏" else "收藏",
                    isActive = isFavorite,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = onFavoriteClick
                )

                ComicActionButton(
                    icon = Icons.AutoMirrored.Filled.Comment,
                    label = formatCount(commentCount),
                    isActive = false,
                    onClick = onCommentClick
                )
            }
        }
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 10000 -> "%.1fw".format(count / 10000f)
            count >= 1000 -> "%.1fk".format(count / 1000f)
            else -> count.toString()
        }
    }

    @Composable
    private fun ComicActionButton(
        icon: ImageVector,
        label: String,
        isActive: Boolean = false,
        activeColor: Color = MaterialTheme.colorScheme.primary,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        val contentColor = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
        val borderColor =
            if (isActive) activeColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant

        OutlinedButton(
            onClick = onClick,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.dp, borderColor),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = modifier
                .height(36.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }

    @Composable
    fun ActionButtonsSection(
        onDownloadClick: () -> Unit = {},
        onReadClick: () -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledTonalButton(
                onClick = onDownloadClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "下载")
            }

            Button(
                onClick = onReadClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "阅读")
            }
        }
    }

    companion object {
        fun start(context: Context, id: String) {
            val intent = Intent(context, ComicInfoActivity2::class.java)
            intent.putExtra("id", id)
            context.startActivity(intent)
        }
    }
}