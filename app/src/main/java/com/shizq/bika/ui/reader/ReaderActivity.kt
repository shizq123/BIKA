package com.shizq.bika.ui.reader

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ReaderAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityReaderBinding
import com.shizq.bika.db.History
import com.shizq.bika.db.Search

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
        viewModel.bookId = intent.getStringExtra("bookId")
        viewModel.order = intent.getIntExtra("order", 1)
        viewModel.chapterTotal = intent.getIntExtra("chapterTotal", 1)

        binding.readerInclude.toolbar.title = ""
        setSupportActionBar(binding.readerInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mAdapter= ReaderAdapter()
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
                binding.pageNumber.text=it.data.pages.total.toString()

                if (it.data.pages.pages == it.data.pages.page) {
                    binding.readerRv.loadMoreEnd()//没有更多数据

                } else {
                    binding.readerRv.loadMoreComplete() //加载完成
                }
            } else {
                if (viewModel.page <= 1) {//当首次加载时出现网络错误
                    showProgressBar(true, "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}")
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
        val historyList=viewModel.getHistory()
        if (historyList.isNotEmpty()) {
            val history = History(
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
            history.id = historyList[0].id
            //这个进行更新 //更新好象要主键
            viewModel.updateHistory(history)//更新记录

        }
    }

}