package com.shizq.bika.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityChatBinding
import com.shizq.bika.network.IReceiveMessage
import com.shizq.bika.network.WebSocketManager

//小程序
class ChatActivity : BaseActivity<ActivityChatBinding,ChatViewModel>() {
    private lateinit var webSocketManager:WebSocketManager

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_chat
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        viewModel.url= intent.getStringExtra("url").toString()+"/socket.io/?EIO=3&transport=websocket"
        binding.chatInclude.toolbar.title=intent.getStringExtra("title").toString()
        setSupportActionBar(binding.chatInclude.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        webSocketManager=WebSocketManager.getInstance()

        webSocketManager.init(viewModel.url,object :IReceiveMessage{
            override fun onConnectSuccess() {
                Log.d("-----------webSocket---成功","")
            }

            override fun onConnectFailed() {
                Log.d("-----------webSocket---失败","")
            }

            override fun onClose() {
                Log.d("-----------webSocket---关闭","")
            }

            override fun onMessage(text: String) {
                //收到消息...（一般是这里处理json）
                Log.d("-----------webSocket---text收到",""+text)
                if (text.substring(0,1)=="0"){
                    //连接成功
                    Log.d("-----------webSocket---text收到","0")
                }
                if (text=="40"){
                    //连接成功
                    Log.d("-----------webSocket---text收到","40")
                    webSocketManager.sendMessage("42[\"init\",\"{\\\"birthday\\\":\\\"2000-01-01T00:00:00.000Z\\\",\\\"character\\\":\\\"https:\\/\\/bidobido.xyz\\/special\\/frame-631.png\\\",\\\"characters\\\":[],\\\"email\\\":\\\"shizqhh\\\",\\\"exp\\\":560,\\\"gender\\\":\\\"bot\\\",\\\"isPunched\\\":true,\\\"level\\\":2,\\\"name\\\":\\\"aaaa\\\",\\\"slogan\\\":\\\"  ？\\\",\\\"title\\\":\\\"萌新\\\",\\\"_id\\\":\\\"630731c2a6fb552ccdb69666\\\",\\\"verified\\\":false}\"]")
                }
                if (text.substring(0,2)=="42"){
                    //连接成功
                    Log.d("-----------webSocket---text收到","消息${text.substring(2)}")
                }
            }

        })

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        webSocketManager.close()
        super.onDestroy()
    }

}