package com.shizq.bika.ui.history

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.data.model.DetailedReadingHistory
import com.shizq.bika.ui.ComicCard
import com.shizq.bika.ui.comicinfo.ComicInfoActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HistoryScreen()
        }
    }

    @Composable
    fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
        val historiesState by viewModel.historiesWithReadChapters.collectAsStateWithLifecycle()
        val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
        HandleDialogState(
            dialogState = dialogState,
            onConfirm = viewModel::confirmDeletion,
            onDismiss = viewModel::dismissDialog
        )
        HistoryContent(
            histories = historiesState,
            onDeleteOneClick = viewModel::requestClearHistory,
            onDeleteAllClick = viewModel::requestClearAllHistory
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HistoryContent(
        histories: List<DetailedReadingHistory>,
        onDeleteOneClick: (comicId: String, title: String) -> Unit = { _, _ -> },
        onDeleteAllClick: () -> Unit = {}
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("历史记录") },
                    actions = {
                        IconButton(onClick = onDeleteAllClick) {
                            Icon(Icons.Default.Delete, contentDescription = "清空全部历史")
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
            ) {
                items(histories) { item ->
                    ComicCard(
                        item, modifier = Modifier
                            .padding(8.dp),
                        onClick = {
                            ComicInfoActivity.start(
                                this@HistoryActivity,
                                item.history.id
                            )
                        },
                        onLongClick = {
                            onDeleteOneClick(item.history.id, item.history.title)
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun HandleDialogState(
        dialogState: DialogState,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        when (dialogState) {
            is DialogState.ConfirmDeleteOne -> {
                ConfirmationDialog(
                    onDismissRequest = onDismiss,
                    onConfirmation = onConfirm,
                    dialogTitle = "删除确认",
                    dialogText = "确定要删除 [${dialogState.title}] 的历史记录吗？"
                )
            }

            is DialogState.ConfirmDeleteAll -> {
                ConfirmationDialog(
                    onDismissRequest = onDismiss,
                    onConfirmation = onConfirm,
                    dialogTitle = "全部删除确认",
                    dialogText = "确定要清空所有历史记录吗？此操作不可撤销。"
                )
            }

            DialogState.Hidden -> { /* 不显示对话框 */
            }
        }
    }

    /**
     * 一个可复用的确认对话框
     */
    @Composable
    fun ConfirmationDialog(
        onDismissRequest: () -> Unit,
        onConfirmation: () -> Unit,
        dialogTitle: String,
        dialogText: String,
        modifier: Modifier = Modifier
    ) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismissRequest,
            title = { Text(text = dialogTitle, style = MaterialTheme.typography.titleLarge) },
            text = { Text(text = dialogText, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = onConfirmation) {
                    Text("确认", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("取消")
                }
            }
        )
    }
}