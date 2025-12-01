package com.shizq.bika.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.isGone
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.database.model.SearchEntity
import com.shizq.bika.databinding.ActivitySearchBinding
import com.shizq.bika.ui.comiclist.ComicListActivity

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
                val searchEntity = SearchEntity(binding.searchView.text.toString())
                viewModel.insertSearch(searchEntity)//添加搜索记录

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

        binding.searchKeyboard.setOnClickListener {
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
                        val searchEntity = SearchEntity(i)
                        viewModel.insertSearch(searchEntity)//添加搜索记录

                        val intent = Intent(this@SearchActivity, ComicListActivity::class.java)
                        intent.putExtra("tag", "search")
                        intent.putExtra("title", i)
                        intent.putExtra("value", i)
                        startActivity(intent)
                    }
                }

            }
        }

        viewModel.allSearchLive.observe(this) {
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
                        viewModel.insertSearch(SearchEntity(i))//添加搜索记录

                        val intent = Intent(this@SearchActivity, ComicListActivity::class.java)
                        intent.putExtra("tag", "search")
                        intent.putExtra("title", i)
                        intent.putExtra("value", i)
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

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchView, 0)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SearchScreen(
        // In a real app, you would pass a ViewModel instance
        // viewModel: SearchViewModel
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        val searchHistory = remember { mutableStateListOf("Comic 1", "Another Search", "Bika") }
        val popularSearches = remember {
            listOf(
                "New Comics",
                "Popular",
                "Action",
                "Adventure",
                "Fantasy",
                "Magic"
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.action_search)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* Handle back navigation */ }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear text")
                            }
                        }
                    }
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Search History Section
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "历史", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { searchHistory.clear() }) {
                                Text("清空")
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            searchHistory.forEach { historyItem ->
                                AssistChip(
                                    onClick = { searchQuery = historyItem },
                                    label = { Text(historyItem) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Popular Searches Section
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "大家都在搜",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            popularSearches.forEach { tag ->
                                AssistChip(
                                    onClick = { searchQuery = tag },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun SearchScreenPreview() {
        // You would wrap this in your app's theme
        MaterialTheme {
            SearchScreen()
        }
    }
}