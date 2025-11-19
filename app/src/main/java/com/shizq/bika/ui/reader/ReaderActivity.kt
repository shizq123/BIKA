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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
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

//阅读漫画页
class ReaderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        hideSystemUI()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val viewModel: ReaderViewModel = viewModel()
            ReaderScreen(viewModel, onBackClick = { finish() })
        }
    }

    @Composable
    fun ReaderScreen(viewModel: ReaderViewModel, onBackClick: () -> Unit) {
        val lazyPagingItems = viewModel.comicPagingFlow.collectAsLazyPagingItems()

        ReaderContent(
            lazyPagingItems,
            onBackClick = onBackClick
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ReaderContent(lazyPagingItems: LazyPagingItems<ComicPage>, onBackClick: () -> Unit) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Black)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        count = lazyPagingItems.itemCount,
                        key = lazyPagingItems.itemKey { it.id },
                    ) { index ->
                        val page = lazyPagingItems[index]
                        if (page != null) {
                            ComicPageItem(page, index)
                        }
                    }

                    // 3. 底部加载状态处理 (Loading More)
                    when (lazyPagingItems.loadState.append) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White)
                                }
                            }
                        }

                        is LoadState.Error -> {
                            item {
                                ErrorFooter(
                                    onRetry = { lazyPagingItems.retry() }
                                )
                            }
                        }

                        else -> {}
                    }
                }

                // 4. 首次加载全屏状态处理 (Loading / Error)
                when (lazyPagingItems.loadState.refresh) {
                    is LoadState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    is LoadState.Error -> {
                        ErrorView(
                            msg = "加载失败",
                            onRetry = { lazyPagingItems.retry() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {}
                }

                // 5. 页面指示器 (可选)
                // Paging 3 获取当前页码比较麻烦，通常简单显示 "已加载 X 张"
                PageIndicator(
                    total = lazyPagingItems.itemCount,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }
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
                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
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
        fun start(context: Context, bookId: String, order: Int, totalEps: Int) {
            val intent = Intent(context, ReaderActivity::class.java)
            intent.putExtra(EXTRA_ID, bookId)
            intent.putExtra(EXTRA_ORDER, order)
            intent.putExtra(EXTRA_TOTAL_EPS, totalEps)
            context.startActivity(intent)
        }
    }
}