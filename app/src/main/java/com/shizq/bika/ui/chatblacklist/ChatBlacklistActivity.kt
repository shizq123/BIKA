package com.shizq.bika.ui.chatblacklist

import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ChatBlackListAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityChatBlacklistBinding

/**
 * 推荐
 */

class ChatBlacklistActivity : BaseActivity<ActivityChatBlacklistBinding, ChatBlacklistViewModel>() {
    private lateinit var mChatBlackListAdapter: ChatBlackListAdapter

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_chat_blacklist
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        binding.blacklistInclude.toolbar.title = "新聊天室"
        setSupportActionBar(binding.blacklistInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mChatBlackListAdapter = ChatBlackListAdapter()
        binding.blacklistRv.layoutManager = LinearLayoutManager(this)
        binding.blacklistRv.adapter = mChatBlackListAdapter


        if (mChatBlackListAdapter.itemCount == 0) {
            showProgressBar(true, "")
            viewModel.getChatBlackList()
        }
//
//        //网络重试点击事件监听
        binding.blacklistLoadLayout.setOnClickListener {
            showProgressBar(true, "")
            viewModel.getChatBlackList()

        }

//        binding.blacklistRv.setOnItemClickListener { _, position ->
//            Toast.makeText(this, mChatRoomsAdapter.getItemData(position).title, Toast.LENGTH_SHORT)
//                .show()
//        }
        binding.blacklistRv.setOnLoadMoreListener {  }

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
        viewModel.liveDataBlackList.observe(this) {
            if (it.limit != 0) {
                binding.blacklistLoadLayout.visibility = ViewGroup.GONE
                mChatBlackListAdapter.clear()
                mChatBlackListAdapter.addData(it.docs)

//                if (it.docs.pages <= it.data.comics.page) {
                    binding.blacklistRv.loadMoreEnd()//没有更多数据
//                } else {
//                    binding.comiclistRv.loadMoreComplete()//加载成功
//                }

            } else {
                showProgressBar(
                    false,
                    "网络错误，点击重试\ncode=${it.statusCode} error=${it.error} message=${it.message}"
                )
            }
        }

    }

    private fun showProgressBar(show: Boolean, string: String = "") {
        binding.blacklistLoadProgressBar.visibility =
            if (show) ViewGroup.VISIBLE else ViewGroup.GONE
        binding.blacklistLoadError.visibility = if (show) ViewGroup.GONE else ViewGroup.VISIBLE
        binding.blacklistLoadText.text = string
        binding.blacklistLoadLayout.isEnabled = !show
    }
}