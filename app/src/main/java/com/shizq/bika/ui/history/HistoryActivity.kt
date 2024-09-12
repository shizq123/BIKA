package com.shizq.bika.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.HistoryAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityHistoryBinding
import com.shizq.bika.database.model.HistoryEntity
import com.shizq.bika.ui.comicinfo.ComicInfoActivity
import kotlinx.coroutines.launch

class HistoryActivity : BaseActivity<ActivityHistoryBinding, HistoryViewModel>() {
    private lateinit var mAdapter: HistoryAdapter

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_history
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        binding.historyInclude.toolbar.title = "历史记录"
        setSupportActionBar(binding.historyInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mAdapter = HistoryAdapter()
        binding.historyRv.layoutManager = LinearLayoutManager(this)
        binding.historyRv.adapter = mAdapter


    }

    //toolbar菜单
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }

            R.id.action_delete -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("删除所有观看记录")
                    .setMessage("确定要删除所有观看记录吗？该操作不可恢复哦")
                    .setPositiveButton("确定") { _, _ ->
                        viewModel.deleteAllHistory()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun initViewObservable() {
        //第一页用livedata 有数据清除原数据重新加载
        viewModel.firstPageHistoryEntityLive.observe(this) {
            mAdapter.clear()
            viewModel.page = 0
            binding.historyRv.loadMoreComplete()
            mAdapter.addData(it)
        }

        //除了第一页后面进行每20条加载更多
        binding.historyRv.setOnLoadMoreListener {
            lifecycleScope.launch {
                val data = viewModel.getAllHistory()
                if (data.isNotEmpty()) {
                    mAdapter.addData(data)
                    binding.historyRv.loadMoreComplete()
                } else {
                    binding.historyRv.loadMoreEnd()
                }
            }

        }

        //点击跳转
        binding.historyRv.setOnItemClickListener { v, position ->
            val data = mAdapter.getItemData(position)
            val intent = Intent(this, ComicInfoActivity::class.java)
            intent.putExtra("id", data.comic_or_game_id)
            intent.putExtra("fileserver", data.fileServer)
            intent.putExtra("imageurl", data.path)
            intent.putExtra("title", data.title)
            intent.putExtra("author", data.author)
            intent.putExtra("totalViews", "")
            startActivity(intent)
        }

        //长按提示删除
        binding.historyRv.setOnItemLongClickListener { v, position ->
            MaterialAlertDialogBuilder(this)
                .setTitle("删除")
                .setMessage("当前记录将被清除，您确定吗？")
                .setPositiveButton("确定") { _, _ ->
                    val data = mAdapter.getItemData(position)
                    val historyEntity = HistoryEntity(
                        data.time,
                        data.title,
                        data.fileServer,
                        data.path,
                        data.comic_or_game,
                        data.author,
                        data.comic_or_game_id,
                        data.sort,
                        data.epsCount,
                        data.pagesCount,
                        data.finished,
                        data.likeCount,
                        data.ep,
                        data.page
                    )
                    historyEntity.id = data.id
                    viewModel.deleteHistory(historyEntity)
                }
                .setNegativeButton("取消", null)
                .show()
            return@setOnItemLongClickListener true
        }


    }
}