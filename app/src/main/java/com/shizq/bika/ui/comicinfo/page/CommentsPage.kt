package com.shizq.bika.ui.comicinfo.page

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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.R
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.data.model.User
import com.shizq.bika.core.designsystem.theme.BikaTheme
import com.shizq.bika.core.ui.RetryableAsyncImage
import com.shizq.bika.ui.comicinfo.comments.CommentsAction
import com.shizq.bika.ui.comicinfo.comments.CommentsState
import com.shizq.bika.ui.comicinfo.comments.CommentsViewModel
import com.shizq.bika.ui.comicinfo.comments.Composer
import kotlinx.coroutines.flow.flowOf

/**
 * 评论 tab 的入口：自己取 VM，不从详情屏往下穿。
 *
 * 作用域仍是 NavEntry 而非 composition —— pager 分支里的
 * `LocalViewModelStoreOwner` 就是这个 NavEntry，页被 dispose 不会清空它的
 * ViewModelStore。所以草稿、点赞覆盖层、回复弹窗在切 tab 和转屏后都还在。
 *
 * 两个收益：详情屏不再持有四个与它无关的参数；评论状态的收集点落在这棵子树里，
 * 打字时的重组不会再穿过整个 Scaffold。
 *
 * key 用 comicId：同一路由里只有一个评论域，换漫画就是换 NavEntry。
 */
@Composable
fun CommentsTab(comicId: String, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<CommentsViewModel, CommentsViewModel.Factory>(
        key = comicId,
    ) { factory ->
        factory.create(comicId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val regularComments = viewModel.regularComments.collectAsLazyPagingItems()
    val replyList = viewModel.replyList.collectAsLazyPagingItems()

    CommentsPage(
        state = state,
        regularComments = regularComments,
        replyList = replyList,
        modifier = modifier,
        dispatch = viewModel::dispatch,
    )
}

/**
 * 评论页。
 *
 * 除 Paging 列表外没有本地状态：草稿、输入器开合、在看哪条回复全在
 * [CommentsState] 里。原先草稿留在 `remember` 而"在看哪条回复"在状态机，
 * 两处真相源的后果是关闭弹窗只重置了本地副本、状态机的值停在最后一次展开。
 *
 * 保持无状态、不取 VM：预览和 UI 测试要能直接喂 [CommentsState]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsPage(
    state: CommentsState,
    regularComments: LazyPagingItems<Comment>,
    replyList: LazyPagingItems<Comment>,
    modifier: Modifier = Modifier,
    dispatch: (CommentsAction) -> Unit = {},
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val focusRequester = remember { FocusRequester() }
    val composer = state.composer

    // 发表成功后由状态机自增 token，refresh() 只能从 UI 侧调用。
    // 跳过初始的 0，否则进页面就会多刷一次。
    LaunchedEffect(state.listRefreshToken) {
        if (state.listRefreshToken > 0) {
            regularComments.refresh()
            if (state.viewingReplies != null) {
                replyList.refresh()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        CommentList(
            pinnedComments = state.pinned,
            regularComments = regularComments,
            modifier = Modifier.fillMaxSize(),
            onToggleLike = { id, liked ->
                dispatch(CommentsAction.ToggleLike(id, liked))
            },
            onReplyClick = { comment ->
                dispatch(
                    CommentsAction.OpenComposer(
                        replyToId = comment.id,
                        replyToName = comment.user.name,
                    )
                )
            },
            onExpandReplies = { dispatch(CommentsAction.ExpandReplies(it)) }
        )

        CommentComposerEntry(
            // 入口条回显草稿：输入器关着时草稿一般是空的，
            // 但发送失败后草稿仍在，此时入口条该显示它而不是占位符
            text = (composer as? Composer.Open)?.draft.orEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter),
            onClick = { dispatch(CommentsAction.OpenComposer()) }
        )

        if (composer is Composer.Open) {
            ModalBottomSheet(
                onDismissRequest = { dispatch(CommentsAction.DismissComposer) },
                sheetState = sheetState,
                dragHandle = null,
                shape = BottomSheetDefaults.HiddenShape,
                modifier = Modifier.imePadding()
            ) {
                ReplyTextField(
                    text = composer.draft,
                    onTextChange = { dispatch(CommentsAction.DraftChanged(it)) },
                    replyingTo = composer.replyToName,
                    sending = composer.sending,
                    error = composer.error,
                    onSend = { dispatch(CommentsAction.Send) },
                    focusRequester = focusRequester
                )
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        val viewingReplies = state.viewingReplies
        if (viewingReplies != null) {
            ReplyDetailsSheet(
                rootComment = viewingReplies,
                replyList = replyList,
                onToggleReplyLike = { id, liked ->
                    dispatch(CommentsAction.ToggleLike(id, liked))
                },
                onDismiss = { dispatch(CommentsAction.CollapseReplies) }
            )
        }
    }
}

@Composable
fun CommentList(
    pinnedComments: List<Comment>,
    regularComments: LazyPagingItems<Comment>,
    modifier: Modifier = Modifier,
    onToggleLike: (commentId: String, currentlyLiked: Boolean) -> Unit,
    onReplyClick: (comment: Comment) -> Unit,
    onExpandReplies: (comment: Comment) -> Unit
) {
    LazyColumn(modifier = modifier) {
        // key 用区段前缀而不是 index：置顶评论同时出现在常规列表里是接口的正常行为，
        // 两者属于列表的不同位置、不是重复数据。掺入 index 会让新数据插入后
        // 所有后续 item 的身份发生变化，item 内的 remember 状态和动画随之错位。
        items(pinnedComments, key = { "pinned_${it.id}" }) { comment ->
            CommentItem(
                comment = comment,
                onToggleLike = { onToggleLike(comment.id, comment.isLiked) },
                onReplyClick = { onReplyClick(comment) },
                onExpandReplies = { onExpandReplies(comment) }
            )
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
        }

        items(
            count = regularComments.itemCount,
            key = { index ->
                val comment = regularComments.peek(index)
                if (comment != null) "regular_${comment.id}" else "placeholder_$index"
            }
        ) { index ->
            regularComments[index]?.let { comment ->
                CommentItem(
                    comment = comment,
                    onToggleLike = { onToggleLike(comment.id, comment.isLiked) },
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

/**
 * 评论输入的入口条。点击后打开真正的输入弹窗，自身不接受输入。
 *
 * 原实现用 disabled 的 TextField 加一层透明 Box 承接点击：无障碍树里
 * 这个控件会被标成"不可用"，再盖一层点击区等于两个信号对着干。
 * 换成语义正确的按钮（Surface(onClick) 自带 Role.Button）。
 */
@Composable
private fun CommentComposerEntry(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text.ifEmpty { "发表你的评论..." },
            style = MaterialTheme.typography.bodyLarge,
            color = if (text.isEmpty()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyDetailsSheet(
    rootComment: Comment,
    replyList: LazyPagingItems<Comment>,
    onToggleReplyLike: (commentId: String, currentlyLiked: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

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


@Composable
fun CommentItem(
    comment: Comment,
    modifier: Modifier = Modifier,
    onToggleLike: () -> Unit = {},
    onExpandReplies: (String) -> Unit = {},
    onReplyClick: (String) -> Unit = {},
    showReplyListButton: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
            // onClickLabel 明确整行的动作是"回复"：行内还有点赞、查看回复两个
            // 可点区，不给标签的话读屏只会报一个无名可点节点
            .clickable(onClickLabel = "回复") { onReplyClick(comment.id) },
    ) {
        // 头像：失败自动重试
        RetryableAsyncImage(
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
                // 原先这里还有一个 MoreHoriz 图标，注释写"举报"但没有任何 clickable：
                // 看起来能点、实际点不动比没有更糟。举报接口在 BikaDataSource 里
                // 也不存在，等功能真正排期时再加回来。
                IconWithText(
                    isLike = comment.isLiked,
                    text = comment.likesCount.toString(),
                    onLikeChanged = onToggleLike
                )
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
    onToggleReplyLike: (commentId: String, currentlyLiked: Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        LazyColumn {
            item(key = "root_${rootComment.id}") {
                CommentItem(
                    comment = rootComment,
                    showReplyListButton = false,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                HorizontalDivider(thickness = 8.dp)
            }
            items(
                replyList.itemCount,
                key = { index ->
                    val reply = replyList.peek(index)
                    if (reply != null) "reply_${reply.id}" else "placeholder_$index"
                }
            ) { index ->
                replyList[index]?.let { reply ->
                    CommentItem(
                        comment = reply,
                        onToggleLike = { onToggleReplyLike(reply.id, reply.isLiked) },
                        onExpandReplies = {},
                        showReplyListButton = false
                    )
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                }
            }
        }
    }
}

/**
 * 点赞数与点赞按钮。
 *
 * 整行作为一个 toggle 语义节点：图标只有 18dp，单独作为点击区远低于
 * 48dp 的最小触达尺寸，所以点击挂在 Row 上并补 [minimumInteractiveComponentSize]。
 * 语义描述随 [isLike] 变化，读屏才能读出当前是"点赞"还是"取消点赞"。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconWithText(
    isLike: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    onLikeChanged: () -> Unit
) {
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(
                role = Role.Button,
                onClickLabel = if (isLike) "取消点赞" else "点赞",
            ) { onLikeChanged() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = if (isLike) {
                painterResource(R.drawable.ic_favorite_24)
            } else {
                painterResource(R.drawable.ic_favorite_border_24)
            },
            // 点击语义与标签已由 Row 承载，图标本身不再重复播报
            contentDescription = null,
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

/**
 * 评论输入框。
 *
 * @param sending 发送中：禁用发送按钮，避免重复提交
 * @param error 上一次发送的失败原因，null 表示无错误。就地显示而不是弹 Toast：
 *   草稿还在框里，错误提示离它越近越好
 */
@Composable
fun ReplyTextField(
    text: String,
    onTextChange: (String) -> Unit,
    replyingTo: String?,
    onSend: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    sending: Boolean = false,
    error: Throwable? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
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
                        if (text.isNotBlank() && !sending) {
                            onSend()
                        }
                    }
                ),
            )

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (error != null) {
                        Text(
                            text = "发表失败，请重试",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                    Button(
                        onClick = { onSend() },
                        enabled = text.isNotBlank() && !sending,
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Text(if (sending) "发送中" else "发送")
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
                text = text,
                onTextChange = { text = it },
                onSend = {},
                focusRequester = remember { FocusRequester() },
                replyingTo = null
            )

            ReplyTextField(
                text = text,
                onTextChange = { text = it },
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
                state = CommentsState(pinned = pinnedComments),
                regularComments = regularComments,
                replyList = regularComments,
            )
        }
    }
}
