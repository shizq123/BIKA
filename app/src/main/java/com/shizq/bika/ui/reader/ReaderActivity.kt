package com.shizq.bika.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ReaderAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.database.model.HistoryEntity
import com.shizq.bika.databinding.ActivityReaderBinding
import kotlinx.coroutines.launch

//阅读漫画页
class ReaderActivity : BaseActivity<ActivityReaderBinding, ReaderViewModel>() {
    private lateinit var mAdapter: ReaderAdapter

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_reader
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }


    override fun initData() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)//屏幕常亮
        viewModel.bookId = intent.getStringExtra("bookId")
        viewModel.order = intent.getIntExtra("order", 1)
        viewModel.totalEps = intent.getIntExtra("totalEps", 1)

        binding.readerInclude.toolbar.title = ""
        setSupportActionBar(binding.readerInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mAdapter = ReaderAdapter()
        binding.readerRv.layoutManager = LinearLayoutManager(this)
        binding.readerRv.adapter = mAdapter

        viewModel.comicsPicture()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu_reader, menu)
        return true
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun initViewObservable() {
        viewModel.liveData_picture.observe(this) {
            if (it.code == 200) {
                binding.readerInclude.toolbar.title = it.data.ep.title
                binding.readerLoadLayout.visibility = View.GONE//隐藏加载进度条页面
                mAdapter.addData(it.data.pages.docs)
                binding.pageNumber.text = it.data.pages.total.toString()

                if (it.data.pages.pages == it.data.pages.page) {
                    binding.readerRv.loadMoreEnd()//没有更多数据

                } else {
                    binding.readerRv.loadMoreComplete() //加载完成
                }
            } else {
                if (viewModel.page <= 1) {//当首次加载时出现网络错误
                    showProgressBar(
                        true,
                        "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                    )
                } else {
                    //不是第一页时 网络错误可能是分页加载时出现的网络错误
                    binding.readerRv.loadMoreFail()
                }
            }
        }

        //分页加载更多
        binding.readerRv.setOnLoadMoreListener {
            viewModel.comicsPicture()
        }

        //网络重试点击事件监听
        binding.readerLoadLayout.setOnClickListener {
            showProgressBar(true, "")
            viewModel.comicsPicture()

        }
    }

    private fun showProgressBar(show: Boolean, string: String) {
        binding.readerLoadProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.readerLoadError.visibility = if (show) View.GONE else View.VISIBLE
        binding.readerLoadText.text = string
        binding.readerLoadLayout.isEnabled = !show
    }

    override fun onPause() {
        super.onPause()
        //保存历史记录
        lifecycleScope.launch {
            val historyList = viewModel.getHistory()
            if (historyList.isNotEmpty()) {
                val historyEntity = HistoryEntity(
                    System.currentTimeMillis(),
                    historyList[0].title,
                    historyList[0].fileServer,
                    historyList[0].path,
                    historyList[0].comic_or_game,
                    historyList[0].author,
                    historyList[0].comic_or_game_id,
                    historyList[0].sort,
                    historyList[0].epsCount,
                    historyList[0].pagesCount,
                    historyList[0].finished,
                    historyList[0].likeCount,
                    historyList[0].ep, //TODO 这里更新章节
                    historyList[0].page //TODO 这里更新页数
                )
                historyEntity.id = historyList[0].id
                //这个进行更新 //更新好象要主键
                viewModel.updateHistory(historyEntity)//更新记录
            }
        }
    }

    companion object {
        internal const val EXTRA_BOOK_ID = "com.shizq.bika.reader.EXTRA_BOOK_ID"
        internal const val EXTRA_ORDER = "com.shizq.bika.reader.EXTRA_ORDER"
        internal const val EXTRA_TOTAL_EPS = "com.shizq.bika.reader.EXTRA_TOTAL_EPS"
        fun start(context: Context, bookId: String, order: Int, totalEps: Int) {
            val intent = Intent(context, ReaderActivity::class.java)
            intent.putExtra(EXTRA_BOOK_ID, bookId)
            intent.putExtra(EXTRA_ORDER, order)
            intent.putExtra(EXTRA_TOTAL_EPS, totalEps)
            context.startActivity(intent)
        }
    }
}