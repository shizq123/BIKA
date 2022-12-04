package com.shizq.bika.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isGone
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shizq.bika.BR
import com.shizq.bika.MyApp
import com.shizq.bika.R
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivitySearchBinding
import com.shizq.bika.db.Search
import com.shizq.bika.ui.account.AccountActivity
import com.shizq.bika.ui.comiclist.ComicListActivity
import com.shizq.bika.utils.SPUtil

class SearchActivity : BaseActivity<ActivitySearchBinding, SearchViewModel>() {

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_search
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        setSupportActionBar(binding.toolbar)
        viewModel.getKey()//显示搜索推荐词
        binding.searchView.requestFocus()
        initListener()


    }

    private fun initListener() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.searchView.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                //监听回车键
                val search=Search(binding.searchView.text.toString())
                viewModel.insertSearch(search)//添加搜索记录

                val intent = Intent(this@SearchActivity, ComicListActivity::class.java)
                intent.putExtra("tag", "search")
                intent.putExtra("title", binding.searchView.text.toString())
                intent.putExtra("value", binding.searchView.text.toString())
                startActivity(intent)

            }
            false
        }

        binding.clearText.setOnClickListener {
            binding.searchView.setText("")
        }

        binding.searchKeyboard.setOnClickListener{
            showKeyboard()
        }
        binding.searchHistoryListClear.setOnClickListener {
            //提示框 清空全部
            MaterialAlertDialogBuilder(this)
                .setTitle("确定清空全部历史记录吗")
                .setPositiveButton("确定") { _, _ ->
                    viewModel.deleteAllSearch()
                }
                .setNegativeButton("取消", null)
                .show()

        }
    }

    override fun initViewObservable() {
        viewModel.liveDataSearchKey.observe(this) {
            binding.searchProgressbar.isGone = true

            if (it.code == 200) {
                val tags = mutableListOf<String>()
                tags.addAll(it.data.keywords)
                for (i in tags) {
                    val chip = Chip(this@SearchActivity)
                    chip.text = i
                    chip.setEnsureMinTouchTargetSize(false)//去除视图的顶部和底部的额外空间
//                        chip.minHeight=0

                    binding.searchTagsList.addView(chip)
                    chip.setOnClickListener {
                        val search=Search(chip.text.toString())
                        viewModel.insertSearch(search)//添加搜索记录

                        val intent = Intent(this@SearchActivity, ComicListActivity::class.java)
                        intent.putExtra("tag", "search")
                        intent.putExtra("title", chip.text.toString())
                        startActivity(intent)
                    }
                }

            }
        }

        viewModel.allSearchLive.observe(this){
            //搜索历史
            if (it.isNotEmpty()) {
                binding.searchHistoryListLayout.visibility = View.VISIBLE
                binding.searchHistoryList.removeAllViews()

                for (i in it) {
                    val chip = Chip(this@SearchActivity)
                    chip.text = i
                    chip.setEnsureMinTouchTargetSize(false)//去除视图的顶部和底部的额外空间
//                        chip.minHeight=0

                    binding.searchHistoryList.addView(chip)
                    chip.setOnClickListener {
                        viewModel.insertSearch(Search(chip.text.toString()))//添加搜索记录

                        val intent = Intent(this@SearchActivity, ComicListActivity::class.java)
                        intent.putExtra("tag", "search")
                        intent.putExtra("title", chip.text.toString())
                        startActivity(intent)
                    }
                }
            } else {
                binding.searchHistoryListLayout.visibility = View.GONE
            }
        }
    }

    private fun hideKeyboard() {
        val view = binding.searchView
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showKeyboard() {

        val imm =getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchView, 0)
    }

}