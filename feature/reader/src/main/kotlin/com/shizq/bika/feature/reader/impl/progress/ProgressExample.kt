package com.shizq.bika.feature.reader.impl.progress

/**
 * 使用示例（仅作参考，实际使用时删除此文件）
 */

/*
// ============ 在 ViewModel 中 ============
class ReaderViewModel : ViewModel() {
    suspend fun persistProgress(page: Int): Boolean {
        // 保存进度到数据库/网络
        return repository.saveProgress(chapterId, page)
    }
}

// ============ 在 ReaderScreen.kt 中 ============
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    initialPage: Int
) {
    val imageList = viewModel.chapterPages.collectAsLazyPagingItems()
    val controller = rememberReaderController()
    
    // 使用进度管理器
    val progressManager = rememberReadingProgressManager(
        controller = controller,
        imageList = imageList,
        initialPage = initialPage,
        onPersist = viewModel::persistProgress,
        config = ProgressConfig(
            initialLoadTimeout = 3_000.milliseconds,
            restoreTimeout = 15_000.milliseconds,
            retryInterval = 100.milliseconds
        )
    )
    
    // 可选：监听状态
    val progressState by progressManager.state.collectAsState()
    when (progressState) {
        is ProgressState.Restoring -> {
            LoadingIndicator("正在恢复进度...")
        }
        is ProgressState.RestoreFailed -> {
            ErrorMessage("恢复失败")
        }
        else -> {
            // 正常显示
        }
    }
    
    // 阅读器内容
    ReaderContent(
        controller = controller,
        imageList = imageList
    )
}

// ============ 高级用法：自定义恢复策略 ============
class CustomRestoreStrategy : ProgressRestoreStrategy {
    override suspend fun restore(
        targetPage: Int,
        dataSource: PageDataSource,
        controller: ReaderController,
        config: ProgressConfig
    ): RestoreResult {
        // 实现你自己的恢复逻辑
        // 例如：只尝试一次，失败就直接返回第 0 页
        
        val loaded = withTimeoutOrNull(config.initialLoadTimeout) {
            dataSource.loadedCountFlow.first { it > 0 }
        }
        
        if (loaded == null) {
            return RestoreResult.Failure("数据加载超时")
        }
        
        controller.scrollToPage(targetPage)
        return RestoreResult.Success(targetPage, attempts = 1)
    }
}
*/
