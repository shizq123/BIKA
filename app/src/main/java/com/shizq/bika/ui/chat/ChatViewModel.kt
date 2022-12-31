package com.shizq.bika.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ChatMessageBean
import com.shizq.bika.network.IReceiveMessage
import com.shizq.bika.network.WebSocketManager
import com.shizq.bika.utils.SPUtil

class ChatViewModel(application: Application) : BaseViewModel(application) {
    var url = ""
    lateinit var webSocketManager: WebSocketManager

    val liveData_connections: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    val liveData_message: MutableLiveData<ChatMessageBean> by lazy {
        MutableLiveData<ChatMessageBean>()
    }

    fun WebSocket(){
        webSocketManager.init(url,object : IReceiveMessage {
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

                if (text=="40"){
                    //收到消息 40 发送init
//                    webSocketManager.sendMessage(user())
                }
                if (text.substring(0,2)=="42"){
                    //收到消息 42 进行解析
                    val key = JsonParser().parse(text.substring(2)).asJsonArray[0].asString
                    val json = JsonParser().parse(text.substring(2)).asJsonArray[1].asJsonObject


                    when(key){
                        "new_connection"->{
                            liveData_connections.postValue("${json.get("connections").asString}人在线")
                        }
                        "receive_notification"->{
                            //42["receive_notification",{"message":"【末雪雪~】正在跟【清埜】悄悄話"}]
                            //悄悄话
                        }
                        "broadcast_message"->{
                            liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))

                        }
                        "broadcast_image"->{
                            liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))

                        }
                        "broadcast_audio"->{
                            liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))
                        }
                        else ->{
                            Log.d("-----------webSocket---text收到","消息${text.substring(2)}")

                        }
                    }

                }
            }

        })
    }

//    var user ={
//        val fileServer= SPUtil.get(this,"user_fileServer","")
//        val path= SPUtil.get(this,"user_path","")
//
//        val map = mutableMapOf(
////            if (fileServer != "") {
////                "fileServer" to fileServer
//            "birthday" to SPUtil.get(this, "user_birthday", ""),
//            "character" to SPUtil.get(this, "user_character", ""),//TODO 网址这里没转义 抓包的是转义的
//            "characters" to ArrayList<Any>(),
//            "email" to SPUtil.get(this, "username", ""),
//            "exp" to SPUtil.get(this, "user_exp", 0),
//            "gender" to SPUtil.get(this, "user_gender", "bot"),
//            "isPunched" to SPUtil.get(this, "setting_punch", false),
//            "level" to SPUtil.get(this, "user_level", 2),
//            "name" to SPUtil.get(this, "user_name", ""),
//            "slogan" to SPUtil.get(this, "user_slogan", ""),
//            "title" to SPUtil.get(this, "user_title", ""),
//            "_id" to SPUtil.get(this, "user_id", ""),
//            "verified" to SPUtil.get(this, "user_verified", false),
//
//            )
//        val m= mutableMapOf(
//            "fileServer" to fileServer,
//            "path" to path
//        )
//        val array=ArrayList<String>()
//        array.add("init")
//        array.add("${Gson().toJson(map)}")
//        "42"+ Gson().toJson(array)
//    }

}