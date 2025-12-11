package com.shizq.bika.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.shizq.bika.core.data.model.Comic

// TODO: 合并到同一个文件 comicCard
@Composable
fun ComicCard(
    comic: Comic,
    onClick: () -> Unit = {},
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(comic.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(110.dp)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // 标题和完结状态
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (comic.finished) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Finished",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = comic.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = comic.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 底部数据统计
                Column {
                    // 标签 (只显示前2个)
                    Row(modifier = Modifier.padding(bottom = 8.dp)) {
                        comic.tags.take(2).fastForEach { tag ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .height(24.dp)
                                    .padding(end = 4.dp),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    true,
                                    borderWidth = 1.dp,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }

                    // 数据行
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatIcon(Icons.Default.Favorite, formatNumber(comic.totalLikes))
                        Spacer(modifier = Modifier.width(12.dp))
                        StatIcon(Icons.Default.Visibility, formatNumber(comic.totalViews))
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${comic.epsCount} 话",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// 辅助组件：小图标+数字
@Composable
fun StatIcon(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

// --- 组件 2: 详情页 (Detail Screen) ---
@OptIn(ExperimentalLayoutApi::class) // FlowRow 需要
@Composable
fun ComicDetailScreen(comic: Comic, onBack: () -> Unit) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* 阅读操作 */ },
                icon = { Icon(Icons.Outlined.Book, null) },
                text = { Text("开始阅读") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            // 头部：大封面 + 渐变背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(comic.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // 底部渐变遮罩，保证文字清晰
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface
                                ),
                                startY = 300f
                            )
                        )
                )

                // 返回按钮
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(50))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            // 内容区域
            Column(modifier = Modifier.padding(16.dp)) {
                // 标题与作者
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "作者: ${comic.author}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // 统计数据栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailStatItem("热度", formatNumber(comic.totalViews))
                    DetailStatItem("喜欢", formatNumber(comic.totalLikes))
                    DetailStatItem("页数", comic.pagesCount.toString())
                    DetailStatItem("章节", comic.epsCount.toString())
                }

                // 分类与标签
                SectionTitle("分类")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    comic.categories.fastForEach { cat ->
                        AssistChip(
                            onClick = {},
                            label = { Text(cat) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Category,
                                    null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("标签")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    comic.tags.forEach { tag ->
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text("#$tag") }
                        )
                    }
                }

                // 底部留白给 FAB
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun DetailStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// 简单的数字格式化工具
fun formatNumber(num: Int): String {
    return when {
        num >= 10000 -> String.format("%.1fw", num / 10000.0)
        num >= 1000 -> String.format("%.1fk", num / 1000.0)
        else -> num.toString()
    }
}

// --- 预览与测试 ---
@Preview(showBackground = true)
@Composable
fun ComicUIPreview() {
    val mockComic = Comic(
        id = "1",
        title = "葬送的芙莉莲 (Frieren)",
        author = "山田钟人 / 阿部司",
        pagesCount = 200,
        epsCount = 128,
        finished = false,
        categories = listOf("冒险", "奇幻", "剧情"),
        tags = listOf("魔法", "治愈", "精灵", "长寿", "日常"),
        totalLikes = 45200,
        totalViews = 1205000,
        coverUrl = "https://placeholder.com/image.jpg" // 实际使用时替换为真实 URL
    )

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            // 预览列表项
            Text("列表项样式:", modifier = Modifier.padding(16.dp))
            ComicCard(comic = mockComic, onClick = {})

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // 预览详情页 (这里用 Box 模拟容器)
            Text("详情页样式 (向下滚动查看):", modifier = Modifier.padding(16.dp))
            Box(modifier = Modifier.height(500.dp)) {
                ComicDetailScreen(comic = mockComic, onBack = {})
            }
        }
    }
}