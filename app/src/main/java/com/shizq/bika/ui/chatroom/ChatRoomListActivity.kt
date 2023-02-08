package com.shizq.bika.ui.chatroom

import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ChatRoomsAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityChatRoomListBinding
import com.shizq.bika.utils.SPUtil

/**
 * 推荐
 */

class ChatRoomListActivity : BaseActivity<ActivityChatRoomListBinding, ChatRoomListViewModel>() {
    private lateinit var mChatRoomsAdapter: ChatRoomsAdapter

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_chat_room_list
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        binding.chatroomInclude.toolbar.title = "新聊天室"
        setSupportActionBar(binding.chatroomInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mChatRoomsAdapter= ChatRoomsAdapter()
        binding.chatroomRv.layoutManager = LinearLayoutManager(this)
        binding.chatroomRv.adapter=mChatRoomsAdapter

        //检查是否有token 没有就进行登录 显示登录提示框
        if (SPUtil.get(this, "chat_token", "") == "") {
            //没有token 登录聊天室
            showProgressBar(true, "获取用户信息...")
            viewModel.chatSignIn()
        } else {
            //有token 获取聊天室列表
            showProgressBar(true, "")
            viewModel.chatRoomList()
        }

//        if (adapter.itemCount < 1) {
//            viewModel.getData()
//        }
//
//        //网络重试点击事件监听
//        binding.collectionsLoadLayout.setOnClickListener {
//            showProgressBar(true, "")
//            viewModel.getData()
//
//        }

        binding.chatroomRv.setOnItemClickListener { _, position ->
            Toast.makeText(this, mChatRoomsAdapter.getItemData(position).title, Toast.LENGTH_SHORT).show()
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
        viewModel.liveDataSignIn.observe(this) {
            //TODO 中途修改密码会登录失败，所以后面要加入账号密码登录，或者直接跳转到登录界面
            if (it.token != "") {
                SPUtil.put(this, "chat_token", it.token)
                viewModel.chatRoomList()
            } else if (it.statusCode == 401 && it.message == "API_ERROR_INVALIID_PASSWORD") {
                Toast.makeText(this, "账号或密码错误", Toast.LENGTH_SHORT).show()
            } else {
                showProgressBar(
                    false,
                    "网络错误，点击重试\ncode=${it.statusCode} error=${it.error} message=${it.message}"
                )
            }
        }
        viewModel.liveDataRoomList.observe(this) {
            if (it.rooms.isNotEmpty()) {
                binding.chatroomLoadLayout.visibility = ViewGroup.GONE
                mChatRoomsAdapter.clear()
                mChatRoomsAdapter.addData(it.rooms)
            } else {
                showProgressBar(
                    false,
                    "网络错误，点击重试\ncode=${it.statusCode} error=${it.error} message=${it.message}"
                )
            }
        }

    }

    private fun showProgressBar(show: Boolean, string: String) {
        binding.chatroomLoadProgressBar.visibility =
            if (show) ViewGroup.VISIBLE else ViewGroup.GONE
        binding.chatroomLoadError.visibility = if (show) ViewGroup.GONE else ViewGroup.VISIBLE
        binding.chatroomLoadText.text = string
        binding.chatroomLoadLayout.isEnabled = !show
    }
}