package com.shizq.bika.ui.comicinfo.page

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shizq.bika.R
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.navigation.DiscoveryAction
import com.shizq.bika.ui.comicinfo.ComicDetail
import com.shizq.bika.ui.comicinfo.ComicSummary
import com.shizq.bika.ui.comicinfo.isComicFullyDownloaded

/**
 * 详情分页。
 *
 * 从 `ComicDetailContent` 的 pager 分支里抽出来，原因不只是那段 lambda 过长：
 * 内联在分支里时，它只能借用外层 Scaffold 的 `rememberCoroutineScope()` 来发起
 * 下载，而那个 scope 会随分页 dispose 一起取消。抽成独立 Composable 后，
 * 下载编排整体移交 ViewModel（见 `ComicInfoViewModel.downloadWholeComic`），
 * 这里只剩纯粹的派生与展示。
 *
 * 用户提示不在这里弹：ViewModel 通过 `MessageReporter` 上报，
 * 由 `BikaApp` 的 Scaffold 统一展示成 Snackbar。
 */
@Composable
fun DetailTab(
    detail: ComicDetail,
    recommendations: List<ComicSummary>,
    downloadTasks: List<DownloadTask>,
    chapterProgress: List<ChapterProgressEntity>,
    onFavoriteClick: () -> Unit,
    onLikedClick: () -> Unit,
    navigationToReader: (order: Int) -> Unit,
    navigationToComicInfo: (String) -> Unit,
    navigationToFeed: (DiscoveryAction) -> Unit,
    onDownloadWholeComic: () -> Unit,
    onTagBlocked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // remember 避免每次重组都重算一遍整个任务列表
    val isDownloaded = remember(downloadTasks, detail.epsCount) {
        isComicFullyDownloaded(downloadTasks, detail.epsCount)
    }

    val lastReadChapter = remember(chapterProgress) {
        chapterProgress.maxByOrNull { it.lastReadAt }
    }
    val lastReadChapterOrder = lastReadChapter?.chapterId ?: 1
    val isContinue = lastReadChapter != null

    var tagToBlock by remember { mutableStateOf<String?>(null) }

    ComicDetailPage(
        detail = detail,
        modifier = modifier,
        recommendations = recommendations,
        isDownloaded = isDownloaded,
        isContinue = isContinue,
        onFavoriteClick = onFavoriteClick,
        onLikedClick = onLikedClick,
        navigationToReader = { navigationToReader(lastReadChapterOrder) },
        navigationToComicInfo = navigationToComicInfo,
        navigationToFeed = navigationToFeed,
        onDownloadClick = onDownloadWholeComic,
        onTagLongClick = { tagToBlock = it },
    )

    // 用局部 val 承接，避免 confirmButton 触发后读到已被置空的 tagToBlock
    val pendingTag = tagToBlock
    if (pendingTag != null) {
        AlertDialog(
            onDismissRequest = { tagToBlock = null },
            title = { Text(stringResource(R.string.tag_block_title)) },
            text = { Text(stringResource(R.string.tag_block_message, pendingTag)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTagBlocked(pendingTag)
                        tagToBlock = null
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToBlock = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
