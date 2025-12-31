package com.shizq.bika.ui.comicinfo.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.shizq.bika.R
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.data.model.User
import com.shizq.bika.ui.theme.BikaTheme
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsPage(
    pinnedComments: List<Comment>,
    regularComments: LazyPagingItems<Comment>,
    replyList: LazyPagingItems<Comment>,
    modifier: Modifier = Modifier,
    onToggleCommentLike: (String) -> Unit = {},
    onExpandReplies: (String) -> Unit = {},
    onPostComment: (text: String, replyToCommentId: String?) -> Unit = { _, _ -> },
) {
    var actionState by remember {
        mutableStateOf<CommentsPageActionState>(CommentsPageActionState.Idle)
    }
    val focusRequester = remember { FocusRequester() }

    Box(modifier = modifier.fillMaxSize()) {
        CommentList(
            pinnedComments = pinnedComments,
            regularComments = regularComments,
            modifier = Modifier.fillMaxSize(),
            onToggleLike = onToggleCommentLike,
            onReplyClick = { comment ->
                actionState = CommentsPageActionState.Replying(comment)
                focusRequester.requestFocus()
            },
            onExpandReplies = { comment ->
                actionState = CommentsPageActionState.ViewingReplies(comment)
                onExpandReplies(comment.id)
            }
        )

        val replyingToComment = (actionState as? CommentsPageActionState.Replying)?.comment

        AnimatedVisibility(
            visible = replyingToComment != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding(),
        ) {
            ReplyTextField(
                replyingTo = replyingToComment?.user?.name,
                onSend = { text ->
                    onPostComment(text, replyingToComment?.id)
                    actionState = CommentsPageActionState.Idle
                },
                focusRequester = focusRequester
            )
        }

        val viewingRepliesForComment =
            (actionState as? CommentsPageActionState.ViewingReplies)?.comment
        if (viewingRepliesForComment != null) {
            ReplyDetailsSheet(
                rootComment = viewingRepliesForComment,
                replyList = replyList,
                onToggleReplyLike = onToggleCommentLike,
                onDismiss = { actionState = CommentsPageActionState.Idle }
            )
        }
    }
}

@Composable
fun CommentList(
    pinnedComments: List<Comment>,
    regularComments: LazyPagingItems<Comment>,
    modifier: Modifier = Modifier,
    onToggleLike: (commentId: String) -> Unit,
    onReplyClick: (comment: Comment) -> Unit,
    onExpandReplies: (comment: Comment) -> Unit
) {
    LazyColumn(modifier = modifier) {
        items(pinnedComments, key = { it.id }) { comment ->
            CommentItem(
                comment = comment,
                onToggleLike = { onToggleLike(comment.id) },
                onReplyClick = { onReplyClick(comment) },
                onExpandReplies = { onExpandReplies(comment) }
            )
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
        }

        items(
            count = regularComments.itemCount,
            key = regularComments.itemKey { it.id }
        ) { index ->
            regularComments[index]?.let { comment ->
                CommentItem(
                    comment = comment,
                    onToggleLike = { onToggleLike(comment.id) },
                    onReplyClick = { onReplyClick(comment) },
                    onExpandReplies = { onExpandReplies(comment) }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyDetailsSheet(
    rootComment: Comment,
    replyList: LazyPagingItems<Comment>,
    onToggleReplyLike: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.statusBarsPadding(),
        sheetState = sheetState,
        dragHandle = null,
        shape = BottomSheetDefaults.HiddenShape
    ) {
        ReplySheetContent(
            rootComment = rootComment,
            replyList = replyList,
            onToggleReplyLike = onToggleReplyLike,
        )
    }
}

private sealed interface CommentsPageActionState {
    data object Idle : CommentsPageActionState
    data class Replying(val comment: Comment) : CommentsPageActionState
    data class ViewingReplies(val comment: Comment) : CommentsPageActionState
}


@Composable
fun CommentItem(
    comment: Comment,
    modifier: Modifier = Modifier,
    onToggleLike: (String) -> Unit = {},
    onExpandReplies: (String) -> Unit = {},
    onReplyClick: (String) -> Unit = {},
    showReplyListButton: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onReplyClick(comment.id) },
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 评论内容
            Text(
                text = comment.content,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 时间/回复
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(comment.createdAt, color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = "回复",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                // 点赞/举报
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconWithText(
                        isLike = comment.isLiked,
                        text = comment.likesCount.toString()
                    ) {
                        onToggleLike(comment.id)
                    }
                    // 举报
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "更多",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (showReplyListButton && comment.totalComments > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                CommentReplyButton(
                    totalComments = comment.totalComments,
                    onReplyClick = { onExpandReplies(comment.id) }
                )
            }
        }
    }
}

/**
 * 底部回复弹窗的内容
 * @param rootComment 被点击的根评论，显示在列表顶部
 * @param replyList 回复列表的 PagingItems
 * @param onToggleReplyLike 给回复点赞的回调
 */
@Composable
fun ReplySheetContent(
    rootComment: Comment,
    replyList: LazyPagingItems<Comment>,
    modifier: Modifier = Modifier,
    onToggleReplyLike: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        LazyColumn {
            item {
                CommentItem(
                    comment = rootComment,
                    showReplyListButton = false,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                HorizontalDivider(thickness = 8.dp)
            }
            items(replyList.itemCount, key = replyList.itemKey { it.id }) { index ->
                replyList[index]?.let { reply ->
                    CommentItem(
                        comment = reply,
                        onToggleLike = onToggleReplyLike,
                        onExpandReplies = {},
                        showReplyListButton = false
                    )
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
fun IconWithText(
    isLike: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    onLikeChanged: () -> Unit
) {
    Row(
        modifier = modifier.clickable(
            role = Role.Button,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { onLikeChanged() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = if (isLike) {
                painterResource(R.drawable.ic_favorite_24)
            } else {
                painterResource(R.drawable.ic_favorite_border_24)
            },
            contentDescription = "Like button",
            modifier = Modifier.size(18.dp),
            tint = if (isLike) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = if (isLike) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun CommentReplyButton(totalComments: Int, onReplyClick: () -> Unit) {
    Card(
        onClick = onReplyClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
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
fun ReplyTextField(
    replyingTo: String?,
    onSend: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val (replyInput, setReplyInput) = remember { mutableStateOf("") }
    val performSend = { text: String ->
        onSend(text)
        setReplyInput("")
    }
    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            TextField(
                value = replyInput,
                onValueChange = setReplyInput,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                interactionSource = interactionSource,
                placeholder = {
                    val placeholderText = if (replyingTo == null) {
                        "留下你的精彩评论吧！"
                    } else {
                        "回复 $replyingTo: "
                    }
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (replyInput.isNotBlank()) {
                            performSend(replyInput)
                        }
                    }
                ),
            )

            AnimatedVisibility(visible = isFocused) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { performSend(replyInput) },
                            enabled = replyInput.isNotBlank(),
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            Text("发送")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ReplyTextFieldPreview() {
    BikaTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            var text by remember { mutableStateOf("") }
            ReplyTextField(
                onSend = {},
                focusRequester = remember { FocusRequester() },
                replyingTo = null
            )

            ReplyTextField(
                onSend = {},
                focusRequester = remember { FocusRequester() },
                replyingTo = "cursus"
            )
        }
    }
}

@Preview
@Composable
fun CommentItemPreview() {
    val sampleUser = User(
        id = "1",
        name = "Bika User",
        gender = "m",
        title = "Knight",
        slogan = "I love Bika",
        level = 10,
        exp = 1000,
        avatar = null,
        characters = emptyList()
    )

    val sampleComment = Comment(
        id = "1",
        content = "This is a sample comment for testing the preview. It should look good in the UI.",
        user = sampleUser,
        totalComments = 5,
        createdAt = "2小时前",
        likesCount = 12,
        isLiked = false
    )

    BikaTheme {
        Surface {
            CommentItem(comment = sampleComment)
        }
    }
}

@Preview
@Composable
fun CommentsPagePreview() {
    val pinnedComments = listOf(
        Comment(
            id = "p1",
            content = "这是置顶评论。它应该出现在最上方。",
            user = User(
                id = "admin",
                name = "管理员",
                gender = "m",
                title = "Admin",
                slogan = "Manager",
                level = 99,
                exp = 99999,
                avatar = null,
                characters = emptyList()
            ),
            totalComments = 2,
            createdAt = "1小时前",
            likesCount = 99,
            isLiked = true
        )
    )

    val regularCommentsList = List(5) { i ->
        Comment(
            id = "r$i",
            content = "这是第 ${i + 1} 条普通评论。这是一些用于填充空间的示例文字。",
            user = User(
                id = "u$i",
                name = "用户 $i",
                gender = "f",
                title = "User",
                slogan = "Hello",
                level = i + 1,
                exp = (i + 1) * 100L,
                avatar = null,
                characters = emptyList()
            ),
            totalComments = i,
            createdAt = "${i + 1}小时前",
            likesCount = i * 5,
            isLiked = false
        )
    }

    val pagingDataFlow = flowOf(PagingData.from(regularCommentsList))
    val regularComments = pagingDataFlow.collectAsLazyPagingItems()

    BikaTheme {
        Surface {
            CommentsPage(
                pinnedComments = pinnedComments,
                regularComments = regularComments,
                replyList = regularComments,
                onToggleCommentLike = {},
                onExpandReplies = {},
            )
        }
    }
}
