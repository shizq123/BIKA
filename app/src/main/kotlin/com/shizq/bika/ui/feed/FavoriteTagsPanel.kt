package com.shizq.bika.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.model.FavoriteTag
import com.shizq.bika.navigation.DiscoveryAction

data class FavoriteTagUiItem(
    val stableKey: String,
    val tag: FavoriteTag,
    val action: DiscoveryAction?,
)

fun FavoriteTag.toUiItem() = FavoriteTagUiItem(
    stableKey = "$actionType:$actionId:$name",
    tag = this,
    action = toAction(),
)

@Composable
fun FavoriteTagsDrawer(
    items: List<FavoriteTagUiItem>,
    currentTag: FavoriteTag? = null,
    onNavigateToFeed: (DiscoveryAction) -> Unit,
    onAddFavorite: (FavoriteTag) -> Unit,
    onRemoveFavorite: (FavoriteTag) -> Unit,
    onRenameRequest: (FavoriteTag) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onAddCustomRequest: () -> Unit,
    onBlockedTagsClick: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    val isCurrentFavorited = remember(items, currentTag) {
        currentTag != null && items.any { it.tag.isSameTag(currentTag) }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 16.dp),
        ) {
            FavoriteTagsHeader(
                onBlockedTagsClick = onBlockedTagsClick,
                onAddCustomRequest = onAddCustomRequest,
                onClose = onClose,
            )

            currentTag?.let { tag ->
                CurrentFavoriteAction(
                    tag = tag,
                    isFavorited = isCurrentFavorited,
                    onAddFavorite = onAddFavorite,
                    onRemoveFavorite = onRemoveFavorite,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "我的收藏",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { isEditMode = !isEditMode },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(if (isEditMode) "完成" else "编辑")
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无收藏，点击上方按钮收藏",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.stableKey },
                    ) { index, item ->
                        FavoriteTagRow(
                            item = item,
                            index = index,
                            itemCount = items.size,
                            isEditMode = isEditMode,
                            onNavigateToFeed = onNavigateToFeed,
                            onRemoveFavorite = onRemoveFavorite,
                            onRenameRequest = onRenameRequest,
                            onMove = onMove,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteTagsHeader(
    onBlockedTagsClick: () -> Unit,
    onAddCustomRequest: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "标签收藏夹",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onBlockedTagsClick) {
            Icon(Icons.Rounded.Block, contentDescription = "标签屏蔽管理")
        }
        IconButton(onClick = onAddCustomRequest) {
            Icon(Icons.Rounded.Add, contentDescription = "新增标签")
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = "关闭")
        }
    }
}

@Composable
private fun CurrentFavoriteAction(
    tag: FavoriteTag,
    isFavorited: Boolean,
    onAddFavorite: (FavoriteTag) -> Unit,
    onRemoveFavorite: (FavoriteTag) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (isFavorited) {
            Button(
                onClick = { onRemoveFavorite(tag) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Star, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("已收藏当前标签 (点击取消)")
            }
        } else {
            OutlinedButton(
                onClick = { onAddFavorite(tag) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.StarBorder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("收藏当前标签")
            }
        }
    }
}

@Composable
private fun FavoriteTagRow(
    item: FavoriteTagUiItem,
    index: Int,
    itemCount: Int,
    isEditMode: Boolean,
    onNavigateToFeed: (DiscoveryAction) -> Unit,
    onRemoveFavorite: (FavoriteTag) -> Unit,
    onRenameRequest: (FavoriteTag) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isEditMode && item.action != null) {
                item.action?.let(onNavigateToFeed)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isEditMode) {
            IconButton(
                onClick = { onRemoveFavorite(item.tag) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.width(8.dp))
        } else {
            Icon(
                imageVector = Icons.Rounded.Bookmarks,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.tag.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.action == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.action == null) {
                Text(
                    text = "无法打开，建议删除",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (isEditMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onRenameRequest(item.tag) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "编辑名称",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = { onMove(index, index - 1) },
                    enabled = index > 0,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "上移")
                }
                IconButton(
                    onClick = { onMove(index, index + 1) },
                    enabled = index < itemCount - 1,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "下移")
                }
            }
        }
    }
}
