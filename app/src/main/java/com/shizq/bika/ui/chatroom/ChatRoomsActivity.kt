package com.shizq.bika.ui.chatroom

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shizq.bika.BR
import com.shizq.bika.MyApp
import com.shizq.bika.R
import com.shizq.bika.adapter.ChatRoomsAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityChatRoomsBinding
import com.shizq.bika.ui.account.AccountActivity
import com.shizq.bika.ui.chat2.ChatActivity
import com.shizq.bika.ui.chatblacklist.ChatBlacklistActivity
import com.shizq.bika.utils.SPUtil

/**
 * 推荐
 */

class ChatRoomsActivity : BaseActivity<ActivityChatRoomsBinding, ChatRoomsViewModel>() {
    private lateinit var mChatRoomsAdapter: ChatRoomsAdapter

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_chat_rooms
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        binding.chatroomInclude.toolbar.title = "新聊天室"
        setSupportActionBar(binding.chatroomInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mChatRoomsAdapter = ChatRoomsAdapter()
        binding.chatroomRv.layoutManager = LinearLayoutManager(this)
        binding.chatroomRv.adapter = mChatRoomsAdapter

        //检查是否有token 没有就进行登录 显示登录提示框
        if (SPUtil.get(this, "chat_token", "") == "") {
            //没有token 登录聊天室
            showProgressBar(true, "获取用户信息...")
            viewModel.chatSignIn()
        } else {
            //有token 获取聊天室列表
            if (mChatRoomsAdapter.itemCount == 0) {
                //这个判断是防止重复请求
                showProgressBar(true, "")
                viewModel.chatRoomList()
            }
        }

        //网络重试点击事件监听
        binding.chatroomLoadLayout.setOnClickListener {
            showProgressBar(true, "")
            viewModel.chatRoomList()

        }

        binding.chatroomRv.setOnItemClickListener { _, position ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("title", mChatRoomsAdapter.getItemData(position).title)
            intent.putExtra("id", mChatRoomsAdapter.getItemData(position).id)
            startActivity(intent)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu_chat_rooms, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.action_blacklist -> {
                startActivity(Intent(this, ChatBlacklistActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun initViewObservable() {
        viewModel.liveDataSignIn.observe(this) {
            //TODO 中途修改密码会登录失败，直接跳转到登录界面
            if (it.token != "") {
                SPUtil.put(this, "chat_token", it.token)
                viewModel.chatRoomList()
//            } else if (it.statusCode == 401 && it.message == "API_ERROR_INVALIID_PASSWORD") {
//                Toast.makeText(this, "账号或密码错误", Toast.LENGTH_SHORT).show()
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle("网络错误")
                    .setMessage("code=${it.statusCode} error=${it.error} message=${it.message}")
                    .setPositiveButton("重新登录") { _, _ ->
                        SPUtil.remove(MyApp.contextBase, "token")
                        startActivity(Intent(this, AccountActivity::class.java))
                        finishAffinity()
                    }
                    .setNegativeButton("取消") { _, _ ->
                        finish()
                    }
                    .show()
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