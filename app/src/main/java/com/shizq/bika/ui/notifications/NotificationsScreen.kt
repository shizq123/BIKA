package com.shizq.bika.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import com.shizq.bika.R
import com.shizq.bika.core.network.model.NotificationDoc
import com.shizq.bika.ui.comment.mine.formatUtcString

@Composable
fun NotificationsScreen(
    onComicClick: (comicId: String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val notifications = viewModel.notificationsFlow.collectAsLazyPagingItems()
    NotificationsContent(
        notifications = notifications,
        onComicClick = onComicClick,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsContent(
    notifications: LazyPagingItems<NotificationDoc>,
    onComicClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的消息") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                notifications.loadState.refresh is LoadState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                notifications.loadState.refresh is LoadState.Error -> {
                    val error = (notifications.loadState.refresh as LoadState.Error).error
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "加载失败: ${error.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { notifications.retry() }) {
                            Text("重试")
                        }
                    }
                }
                notifications.itemCount == 0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MailOutline,
                            contentDescription = "无消息",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无消息",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            count = notifications.itemCount,
                            key = notifications.itemKey { it.id }
                        ) { index ->
                            notifications[index]?.let { item ->
                                NotificationItem(
                                    notification = item,
                                    onComicClick = onComicClick
                                )
                            }
                        }

                        if (notifications.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: NotificationDoc,
    onComicClick: (comicId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasRedirect = !notification.redirectId.isNullOrEmpty()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = hasRedirect) {
                notification.redirectId?.let { onComicClick(it) }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧：头像
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = notification.sender?.avatar?.originalImageUrl
                    ?: notification.sender?.avatar?.imageUrl

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .placeholder(R.drawable.placeholder_avatar_2)
                        .error(R.drawable.placeholder_avatar_2)
                        .build(),
                    contentDescription = "Sender Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )

                // 头像框 / 立绘挂件
                notification.sender?.character?.let { characterUrl ->
                    if (characterUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(characterUrl)
                                .build(),
                            contentDescription = "Avatar Character",
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 中间：正文与详情
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.Top)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = notification.sender?.name ?: (if (notification.system) "系统" else "匿名"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (notification.system) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )

                    // 称号/头衔
                    val title = notification.sender?.title
                    if (!title.isNullOrEmpty()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // 系统消息角标
                    if (notification.system) {
                        Text(
                            text = "SYSTEM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatUtcString(notification.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // 右侧：如有封面展示封面
            notification.cover?.let { coverMedia ->
                val coverUrl = coverMedia.originalImageUrl.ifEmpty { coverMedia.imageUrl }
                if (coverUrl.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverUrl)
                            .build(),
                        contentDescription = "Notification Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 50.dp, height = 68.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }
            }
        }
    }
}
