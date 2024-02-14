package com.shizq.bika.ui.collections

import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.CollectionsAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityCollectionsBinding
import com.shizq.bika.network.Result
import kotlinx.coroutines.launch

/**
 * 推荐
 */

class CollectionsActivity : BaseActivity<ActivityCollectionsBinding, CollectionsViewModel>() {
    private lateinit var adapter: CollectionsAdapter

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_collections
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        binding.collectionsInclude.toolbar.title = "推荐"
        setSupportActionBar(binding.collectionsInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = CollectionsAdapter(this)
        binding.collectionsRv.layoutManager = LinearLayoutManager(this)
        binding.collectionsRv.adapter = adapter

        showProgressBar(true, "")
        if (adapter.itemCount < 1) {
            viewModel.getData()
        }

        //网络重试点击事件监听
        binding.collectionsLoadLayout.setOnClickListener {
            showProgressBar(true, "")
            viewModel.getData()

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun initViewObservable() {
        lifecycleScope.launch {
            viewModel.collections.collect {
                when (it) {
                    is Result.Success -> {
                        binding.collectionsLoadLayout.visibility = ViewGroup.GONE
                        if (adapter.itemCount < 1) {
                            adapter.addData(it.data.collections)
                        }
                    }

                    is Result.Error -> {
                        showProgressBar(
                            false,
                            "网络错误，点击重试\ncode=${it.code} error=${it.error} message=${it.message}"
                        )
                    }

                    is Result.Loading -> {}
                    else -> {}
                }
            }
        }
    }

    private fun showProgressBar(show: Boolean, string: String) {
        binding.collectionsLoadProgressBar.visibility =
            if (show) ViewGroup.VISIBLE else ViewGroup.GONE
        binding.collectionsLoadError.visibility = if (show) ViewGroup.GONE else ViewGroup.VISIBLE
        binding.collectionsLoadText.text = string
        binding.collectionsLoadLayout.isEnabled = !show
    }
}