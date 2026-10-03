package com.shizq.bika.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shizq.bika.R
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
fun FavoriteTagsContent(
    items: List<FavoriteTagUiItem>,
    modifier: Modifier = Modifier,
    currentTag: FavoriteTag? = null,
    onNavigateToFeed: (DiscoveryAction) -> Unit,
    onAddFavorite: (FavoriteTag) -> Unit,
    onRemoveFavorite: (FavoriteTag) -> Unit,
    onRenameRequest: (FavoriteTag) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onAddCustomRequest: () -> Unit,
    onBlockedTagsClick: () -> Unit = {},
    onClose: () -> Unit,
) {
    var isEditMode by remember { mutableStateOf(false) }
    val isCurrentFavorited = remember(items, currentTag) {
        currentTag != null && items.any { it.tag.isSameTag(currentTag) }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp, max = 720.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 8.dp),
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
                    text = stringResource(R.string.feed_favorites_my),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { isEditMode = !isEditMode },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        stringResource(
                            if (isEditMode) R.string.feed_favorites_done
                            else R.string.feed_favorites_edit
                        )
                    )
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
                        text = stringResource(R.string.feed_favorites_empty),
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
            text = stringResource(R.string.feed_favorites_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onBlockedTagsClick) {
            Icon(
                Icons.Rounded.Block,
                contentDescription = stringResource(R.string.feed_favorites_manage_blocked),
            )
        }
        IconButton(onClick = onAddCustomRequest) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = stringResource(R.string.feed_favorites_add_tag),
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.feed_favorites_close),
            )
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
                Text(stringResource(R.string.feed_favorites_remove_current))
            }
        } else {
            OutlinedButton(
                onClick = { onAddFavorite(tag) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.StarBorder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feed_favorites_add_current))
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
                    contentDescription = stringResource(R.string.feed_favorites_delete),
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
                    text = stringResource(R.string.feed_favorites_unavailable),
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
                        contentDescription = stringResource(R.string.feed_favorites_rename),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = { onMove(index, index - 1) },
                    enabled = index > 0,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.feed_favorites_move_up),
                    )
                }
                IconButton(
                    onClick = { onMove(index, index + 1) },
                    enabled = index < itemCount - 1,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.feed_favorites_move_down),
                    )
                }
            }
        }
    }
}
