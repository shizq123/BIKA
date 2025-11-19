package com.shizq.bika.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.shizq.bika.R
import com.shizq.bika.paging.ComicPage
import com.shizq.bika.paging.PagingMetadata
import com.shizq.bika.ui.reader.bar.BottomBar
import com.shizq.bika.ui.reader.bar.TopBar
import com.shizq.bika.ui.reader.gesture.GestureAction
import com.shizq.bika.ui.reader.gesture.readerControls
import com.shizq.bika.ui.reader.gesture.rememberGestureState
import kotlinx.coroutines.launch

//阅读漫画页
class ReaderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val viewModel: ReaderViewModel = viewModel()
            ReaderScreen(viewModel, onBackClick = { finish() })
        }
    }

    @Composable
    fun ReaderScreen(viewModel: ReaderViewModel, onBackClick: () -> Unit) {
        val lazyPagingItems = viewModel.comicPagingFlow.collectAsLazyPagingItems()
        val title by PagingMetadata.title.collectAsStateWithLifecycle()
        val pageCount by PagingMetadata.totalElements.collectAsStateWithLifecycle()
        val currentPageIndex by viewModel.currentPage.collectAsStateWithLifecycle()
        ReaderContent(
            lazyPagingItems,
            title = title,
            currentPageIndex = currentPageIndex,
            onPageChange = { viewModel.currentPage.value = it },
            pageCount = pageCount,
            onBackClick = onBackClick
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ReaderContent(
        lazyPagingItems: LazyPagingItems<ComicPage>,
        onBackClick: () -> Unit,
        pageCount: Int,
        currentPageIndex: Int,
        onPageChange: (Int) -> Unit,
        title: String
    ) {
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        var showMenu by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(showMenu) {
            if (showMenu) {
                showSystemUI()
            } else {
                hideSystemUI()
            }
        }
        ReaderScaffold(
            showMenu = showMenu,
            topBar = {
                TopBar(title = { Text(title) }, onBackClick = onBackClick)
            },
            bottomBar = {
                BottomBar(
                    progressIndicator = {
                        if (pageCount > 0) {
                            Text(text = "${currentPageIndex + 1} / $pageCount")
                        }
                    },
                    progressSlider = {
                        Slider(
                            value = currentPageIndex.toFloat(),
                            onValueChange = {
                                onPageChange(it.toInt())
                            },
                            onValueChangeFinished = {
                                scope.launch {
                                    listState.animateScrollToItem(currentPageIndex)
                                }
                            },
                            valueRange = 0f..(pageCount.coerceAtLeast(0)).toFloat()
                        )
                    },
                    startActions = {
                        IconButton(onClick = { /* 打开目录 */ }) {
                            Icon(Icons.Default.Menu, "目录")
                        }
                    },
                    middleActions = {
                        IconButton(onClick = { /* 上一话 */ }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "上一话")
                        }
                        IconButton(onClick = { /* 下一话 */ }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "下一话")
                        }
                    },
                    endActions = {
                        IconButton(onClick = { /* 打开设置 */ }) {
                            Icon(Icons.Default.Settings, "设置")
                        }
                    }
                )
            },
            floatingIndicators = {
                val current = listState.firstVisibleItemIndex + 1
                Text(
                    text = "$current / $pageCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            },
            gestureHost = {
                val gestureState = rememberGestureState(centerRatio = 0.4f)
                val screenHeight = maxHeight
                Box(
                    Modifier
                        .fillMaxSize()
                        .readerControls(state = gestureState) { action ->
                            when (action) {
                                GestureAction.ToggleMenu -> showMenu = !showMenu
                                GestureAction.PrevPage -> {
                                    // 逻辑：向上滚动一屏
                                    scope.launch {
                                        listState.animateScrollBy(-screenHeight.value)
                                    }
                                }

                                GestureAction.NextPage -> {
                                    // 逻辑：向下滚动一屏
                                    scope.launch {
                                        listState.animateScrollBy(screenHeight.value)
                                    }
                                }

                                GestureAction.None -> {}
                            }
                        }
                )
            },
            content = {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = lazyPagingItems.itemCount,
                        key = lazyPagingItems.itemKey { it.id },
                    ) { index ->
                        val page = lazyPagingItems[index]
                        if (page != null) {
                            ComicPageItem(page, index)
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun ComicPageItem(
        page: ComicPage,
        index: Int,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current

        val imageRequest = remember(page.url) {
            ImageRequest.Builder(context)
                .data(page.url)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
                .size(Size.ORIGINAL)
                .build()
        }

        // 外层容器
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = "Page ${index + 1}",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            ) {
                val state = painter.state

                when (state) {
                    is AsyncImagePainter.State.Loading, is AsyncImagePainter.State.Empty -> {
                        // === 加载中状态 ===
                        // 使用 Box 包裹，以便在占位图之上叠加文字
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center // 1. 关键：设定内容左侧垂直居中对齐
                        ) {
                            // A. 占位图 (铺满)
                            Image(
                                painter = painterResource(id = R.drawable.placeholder_transparent_low),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // B. 页码文字 (显示在左侧)
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.Gray
                            )
                        }
                    }

                    is AsyncImagePainter.State.Success -> {
                        SubcomposeAsyncImageContent()
                    }

                    is AsyncImagePainter.State.Error -> {
                    }
                }
            }
        }
    }

    @Composable
    fun PageIndicator(total: Int, modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = Color.Black.copy(alpha = 0.6f),
            contentColor = Color.White
        ) {
            Text(
                text = "Total: $total",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    fun ErrorView(msg: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "加载失败，点击重试",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }

    @Composable
    fun ErrorFooter(onRetry: () -> Unit) {
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("加载失败，点击重试")
        }
    }

    private fun hideSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

//    override fun initViewObservable() {
//        viewModel.liveData_picture.observe(this) {
//            if (it.code == 200) {
//                binding.readerInclude.toolbar.title = it.data.ep.title
//                binding.readerLoadLayout.visibility = View.GONE//隐藏加载进度条页面
//                mAdapter.addData(it.data.pages.docs)
//                binding.pageNumber.text = it.data.pages.total.toString()
//
//                if (it.data.pages.pages == it.data.pages.page) {
//                    binding.readerRv.loadMoreEnd()//没有更多数据
//
//                } else {
//                    binding.readerRv.loadMoreComplete() //加载完成
//                }
//            } else {
//                if (viewModel.page <= 1) {//当首次加载时出现网络错误
//                    showProgressBar(
//                        true,
//                        "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
//                    )
//                } else {
//                    //不是第一页时 网络错误可能是分页加载时出现的网络错误
//                    binding.readerRv.loadMoreFail()
//                }
//            }
//        }
//
//        //分页加载更多
//        binding.readerRv.setOnLoadMoreListener {
//            viewModel.comicsPicture()
//        }
//
//        //网络重试点击事件监听
//        binding.readerLoadLayout.setOnClickListener {
//            showProgressBar(true, "")
//            viewModel.comicsPicture()
//
//        }
//    }
//
//    private fun showProgressBar(show: Boolean, string: String) {
//        binding.readerLoadProgressBar.visibility = if (show) View.VISIBLE else View.GONE
//        binding.readerLoadError.visibility = if (show) View.GONE else View.VISIBLE
//        binding.readerLoadText.text = string
//        binding.readerLoadLayout.isEnabled = !show
//    }
//
//    override fun onPause() {
//        super.onPause()
//        //保存历史记录
//        lifecycleScope.launch {
//            val historyList = viewModel.getHistory()
//            if (historyList.isNotEmpty()) {
//                val historyEntity = HistoryEntity(
//                    System.currentTimeMillis(),
//                    historyList[0].title,
//                    historyList[0].fileServer,
//                    historyList[0].path,
//                    historyList[0].comic_or_game,
//                    historyList[0].author,
//                    historyList[0].comic_or_game_id,
//                    historyList[0].sort,
//                    historyList[0].epsCount,
//                    historyList[0].pagesCount,
//                    historyList[0].finished,
//                    historyList[0].likeCount,
//                    historyList[0].ep, //TODO 这里更新章节
//                    historyList[0].page //TODO 这里更新页数
//                )
//                historyEntity.id = historyList[0].id
//                //这个进行更新 //更新好象要主键
//                viewModel.updateHistory(historyEntity)//更新记录
//            }
//        }
//    }

    companion object {
        internal const val EXTRA_ID = "com.shizq.bika.reader.EXTRA_BOOK_ID"
        internal const val EXTRA_ORDER = "com.shizq.bika.reader.EXTRA_ORDER"
        internal const val EXTRA_TOTAL_EPS = "com.shizq.bika.reader.EXTRA_TOTAL_EPS"
        internal const val EXTRA_TITLE = "com.shizq.bika.reader.EXTRA_TITLE"
        fun start(context: Context, bookId: String, order: Int, totalEps: Int, title: String) {
            val intent = Intent(context, ReaderActivity::class.java)
            intent.putExtra(EXTRA_ID, bookId)
            intent.putExtra(EXTRA_ORDER, order)
            intent.putExtra(EXTRA_TOTAL_EPS, totalEps)
            intent.putExtra(EXTRA_TITLE, title)
            context.startActivity(intent)
        }
    }
}