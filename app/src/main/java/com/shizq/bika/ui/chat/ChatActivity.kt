package com.shizq.bika.ui.chat

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ChatAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.bean.ChatMessageBean
import com.shizq.bika.databinding.ActivityChatBinding
import com.shizq.bika.network.WebSocketManager
import com.shizq.bika.widget.UserViewDialog

//聊天室
//消息是websocket实现，消息是实时，不会留记录,网络不好会丢失消息
class ChatActivity : BaseActivity<ActivityChatBinding,ChatViewModel>() {
    private lateinit var adapter:ChatAdapter
    private lateinit var chatModelList: ArrayList<ChatMessageBean>

    private lateinit var userViewDialog: UserViewDialog

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

        chatModelList= ArrayList()
        adapter= ChatAdapter(chatModelList)
        binding.chatRv.layoutManager = LinearLayoutManager(this)
        binding.chatRv.adapter = adapter

        userViewDialog = UserViewDialog(this)

        viewModel.WebSocket()
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

    override fun initViewObservable() {
        viewModel.liveData_connections.observe(this){
            binding.chatInclude.toolbar.subtitle=it
        }
        viewModel.liveData_message.observe(this){
            chatModelList.add(it)
            adapter.notifyItemInserted(chatModelList.size-1)

            //加个判断 当前显示的条目是否是最后一条 是就正常加载，不是就右下角显示几条消息，类似qq
            binding.chatRv.scrollToPosition(chatModelList.size-1)
        }
    }


    override fun onDestroy() {
        viewModel.webSocketManager.close()
        super.onDestroy()
    }

}
