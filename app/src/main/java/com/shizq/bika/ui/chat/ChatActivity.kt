package com.shizq.bika.ui.chat

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ChatAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityChatBinding
import com.shizq.bika.network.WebSocketManager
import com.shizq.bika.widget.UserViewDialog

//聊天室
//消息是websocket实现，消息是实时，不会留记录,网络不好会丢失消息
class ChatActivity : BaseActivity<ActivityChatBinding,ChatViewModel>() {
    private lateinit var adapter:ChatAdapter
    private lateinit var userViewDialog: UserViewDialog
    var chatRvBottom=false//false表示底部

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_chat
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    @SuppressLint("ResourceType")
    override fun initData() {
        viewModel.url= intent.getStringExtra("url").toString()+"/socket.io/?EIO=3&transport=websocket"
        binding.chatInclude.toolbar.title=intent.getStringExtra("title").toString()
        setSupportActionBar(binding.chatInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel.webSocketManager=WebSocketManager.getInstance()

        adapter=ChatAdapter()
        binding.chatRv.layoutManager = LinearLayoutManager(this)
        binding.chatRv.adapter = adapter

        userViewDialog = UserViewDialog(this)

        viewModel.WebSocket()
        initListener()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.action_setting ->{

            }

        }
        return super.onOptionsItemSelected(item)
    }

    fun initListener(){
        binding.chatRv.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                chatRvBottom = recyclerView.canScrollVertically(1)//判断是否到底部 false是底部

                if (!chatRvBottom) {
                    binding.chatRvBottomBtn.visibility=View.GONE
                } else {
                    binding.chatRvBottomBtn.visibility=View.VISIBLE
                }

            }
        })
        binding.chatRvBottomBtn.setOnClickListener {
            chatRvBottom=false
            binding.chatRvBottomBtn.visibility=View.GONE
            binding.chatRv.scrollToPosition(adapter.data.size - 1)
        }

        binding.chatRv.setOnItemChildClickListener { view, position ->
            val id= view.id
            val data =adapter.getItemData(position)
            if (id==R.id.chat_avatar_layout_l){
                //头像点击事件 查看用户信息
                //聊天信息携带的用户信息不全 可以进行网络获取 以后再说
                userViewDialog.showUserDialog(data)
            }
            if (id==R.id.chat_name_l){
                //名字点击事件 用于 @
                Toast.makeText(this,"@${data.name}",Toast.LENGTH_SHORT).show()
            }

            if (id==R.id.chat_message_layout_l){
                //消息点击事件 用于 回复
                Toast.makeText(this,"回复-${data.message}",Toast.LENGTH_SHORT).show()

            }
        }

        binding.chatRv.setOnItemChildLongClickListener { view, position ->
            if (view.id==R.id.chat_avatar_layout_l){
                //头像点击事件 用于 @
                Toast.makeText(this,"@${adapter.getItemData(position).name}",Toast.LENGTH_SHORT).show()
            }
            true
        }

    }

    override fun initViewObservable() {
        viewModel.liveData_connections.observe(this){
            binding.chatInclude.toolbar.subtitle=it
        }
        viewModel.liveData_message.observe(this){
            adapter.addData(it)
            if (!chatRvBottom) {
                binding.chatRv.scrollToPosition(adapter.data.size - 1)
            }

        }

    }


    override fun onDestroy() {
        viewModel.webSocketManager.close()
        super.onDestroy()
    }

}
