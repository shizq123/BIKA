package com.shizq.bika.ui.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import com.shizq.bika.R

@Composable
fun DashboardDrawerContent(
    userProfile: UserProfileUiState,
    modifier: Modifier = Modifier,
    onCheckInClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onCommentsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val menuItems = listOf(
        DrawerMenuEntry(
            label = "历史记录",
            icon = DrawerIcon.Res(R.drawable.ic_history),
            testTag = DashboardDrawerTags.History,
            onClick = onHistoryClick
        ),
        DrawerMenuEntry(
            label = "我的收藏",
            icon = DrawerIcon.Vector(Icons.Filled.Favorite),
            testTag = DashboardDrawerTags.Favourite,
            onClick = onFavouriteClick
        ),
        DrawerMenuEntry(
            label = "我的消息",
            icon = DrawerIcon.Vector(Icons.Filled.Email),
            testTag = DashboardDrawerTags.Notifications,
            onClick = onNotificationsClick
        ),
        DrawerMenuEntry(
            label = "我的评论",
            icon = DrawerIcon.Vector(Icons.AutoMirrored.Filled.Comment),
            testTag = DashboardDrawerTags.Comments,
            onClick = onCommentsClick
        ),
        DrawerMenuEntry(
            label = "设置",
            icon = DrawerIcon.Vector(Icons.Filled.Settings),
            testTag = DashboardDrawerTags.Settings,
            onClick = onSettingsClick
        )
    )
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item(key = "profile") {
            UserProfileCard(
                state = userProfile,
                onCheckInClick = onCheckInClick,
                onEditProfileClick = onEditProfileClick
            )
        }
        item(key = "divider") {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        items(
            items = menuItems,
            key = { it.testTag }
        ) { item ->
            DrawerMenuItem(
                label = item.label,
                icon = item.icon,
                onClick = item.onClick,
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}

@Composable
private fun UserProfileCard(
    state: UserProfileUiState,
    modifier: Modifier = Modifier,
    onCheckInClick: () -> Unit,
    onEditProfileClick: () -> Unit,
) {
    when (state) {
        UserProfileUiState.Loading -> {
            UserProfileLoadingCard(modifier = modifier)
        }

        is UserProfileUiState.Error -> {
            UserProfileErrorCard(modifier = modifier)
        }

        is UserProfileUiState.Success -> {
            UserProfileSuccessCard(
                user = state.user,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun UserProfileSuccessCard(
    user: User,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageRequest = remember(user.avatarUrl, context) {
        ImageRequest.Builder(context)
            .data(user.avatarUrl)
            .placeholder(R.drawable.placeholder_avatar_2)
            .error(R.drawable.placeholder_avatar_2)
            .build()
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(DashboardDrawerTags.UserProfile),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "${user.name}的头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = user.levelDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (user.title.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = user.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = user.gender.value,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = user.slogan.ifBlank { "这个人很懒，什么都没写" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UserProfileLoadingCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(DashboardDrawerTags.UserProfile),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "加载中...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// TODO: 添加重试操作
@Composable
private fun UserProfileErrorCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(DashboardDrawerTags.UserProfile),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "用户信息加载失败",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun DrawerMenuItem(
    label: String,
    icon: DrawerIcon,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        selected = selected,
        onClick = onClick,
        icon = {
            when (icon) {
                is DrawerIcon.Res -> Icon(
                    painter = painterResource(icon.id),
                    contentDescription = null
                )

                is DrawerIcon.Vector -> Icon(
                    imageVector = icon.imageVector,
                    contentDescription = null
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

private data class DrawerMenuEntry(
    val label: String,
    val icon: DrawerIcon,
    val testTag: String,
    val onClick: () -> Unit
)

private sealed interface DrawerIcon {
    data class Res(@DrawableRes val id: Int) : DrawerIcon
    data class Vector(val imageVector: ImageVector) : DrawerIcon
}

private object DashboardDrawerTags {
    const val UserProfile = "dashboard:userProfile"
    const val History = "dashboard:drawer:history"
    const val Favourite = "dashboard:drawer:favourite"
    const val Notifications = "dashboard:drawer:notifications"
    const val Comments = "dashboard:drawer:comments"
    const val Settings = "dashboard:drawer:settings"
}
// TODO: 编辑资料放到侧边栏，签到放到主页

//     Row(
//                        horizontalArrangement = Arrangement.End,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        TextButton(onClick = onEditProfile) {
//                            Text(text = "修改资料")
//                        }
//
//                        Spacer(modifier = Modifier.width(8.dp))
//
//                        Button(
//                            onClick = onCheckInClick,
//                            enabled = !user.hasCheckedIn,
//                            colors = ButtonDefaults.buttonColors(
//                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
//                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
//                            ),
//                            modifier = Modifier.testTag("dashboard:checkIn"),
//                        ) {
//                            Text(text = if (user.hasCheckedIn) "已打卡" else "打卡")
//                        }
//                    }