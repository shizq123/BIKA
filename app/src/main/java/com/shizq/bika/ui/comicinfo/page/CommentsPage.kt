package com.shizq.bika.ui.comicinfo.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.shizq.bika.R
import com.shizq.bika.core.data.model.Comment

data class Badge(
    val text: String,
    val textColor: Color,
    val backgroundColor: Color
)

@Composable
fun CommentsPage(
    pinnedComments: List<Comment>,
    regularComments: LazyPagingItems<Comment>,
    modifier: Modifier = Modifier,
    onToggleCommentLike: (String) -> Unit = {}
) {
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(pinnedComments) { item ->
                CommentItem(comment = item)
            }
            items(regularComments.itemCount, key = regularComments.itemKey { it.id }) { item ->
                regularComments[item]?.let {
                    CommentItem(comment = it, onToggleCommentLike = onToggleCommentLike)
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment, onToggleCommentLike: (String) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 头像
        AsyncImage(
            model = comment.user.avatar,
            contentDescription = "用户头像",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            // 用户信息行
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.user.name,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 标签
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
//                    comment.user.badges.forEach { badge ->
//                        CommentBadge(badge)
//                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 评论内容
            Text(
                text = comment.content,
                fontSize = 15.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 时间、地点和操作行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(comment.createdAt, color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = "回复",
                    color = Color.DarkGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                // 点赞/举报
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconWithText(
                        isLike = false,
                        text = comment.likesCount.toString()
                    ) {
                        onToggleCommentLike(comment.id)
                    }
                    // 举报
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "更多",
                        modifier = Modifier.size(18.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (comment.totalComments > 0) {
                CommentReplyButton(totalComments = comment.totalComments) {}
            }
        }
    }
}

@Composable
fun IconWithText(
    isLike: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    onLikeChanged: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.clickable { onLikeChanged(!isLike) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconToggleButton(
            checked = isLike,
            onCheckedChange = onLikeChanged
        ) {
            Icon(
                painter = if (isLike) {
                    painterResource(R.drawable.ic_favorite_24)
                } else {
                    painterResource(R.drawable.ic_favorite_border_24)
                },
                contentDescription = "Like button",
                modifier = Modifier.size(18.dp),
                tint = if (isLike) Color.Red else Color.Gray
            )
        }
        Text(
            text = text,
            color = Color.Gray,
            fontSize = 13.sp
        )
    }
}

@Composable
fun CommentReplyButton(totalComments: Int, onReplyClick: () -> Unit) {
    Card(
        onClick = onReplyClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "共${totalComments}条回复 >",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun CommentBadge(badge: Badge) {
    Text(
        text = badge.text,
        color = badge.textColor,
        fontSize = 10.sp,
        modifier = Modifier
            .background(color = badge.backgroundColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun CommentInputField() {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFF0F0F0), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "你猜我的评论区在等待谁？",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
//            Icon(
//                painter = painterResource(id = R.drawable.ic_smile), // 请准备一个笑脸图标
//                contentDescription = "表情",
//                tint = Color.Gray,
//                modifier = Modifier.size(22.dp)
//            )
        }
    }
}