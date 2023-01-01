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
                    webSocketManager.sendMessage(user())
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
                            liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))
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

    var user ={
        val fileServer= SPUtil.get(application,"user_fileServer","")
        val path= SPUtil.get(application,"user_path","")
        val character= SPUtil.get(application,"user_character","")

        val map = mutableMapOf(
            "birthday" to SPUtil.get(application, "user_birthday", ""),
            "characters" to ArrayList<Any>(),
            "email" to SPUtil.get(application, "username", ""),
            "exp" to SPUtil.get(application, "user_exp", 0),
            "gender" to SPUtil.get(application, "user_gender", "bot"),
            "isPunched" to SPUtil.get(application, "setting_punch", false),
            "level" to SPUtil.get(application, "user_level", 2),
            "name" to SPUtil.get(application, "user_name", ""),
            "slogan" to SPUtil.get(application, "user_slogan", ""),
            "title" to SPUtil.get(application, "user_title", ""),
            "_id" to SPUtil.get(application, "user_id", ""),
            "verified" to SPUtil.get(application, "user_verified", false),

            )

        if (fileServer != "") {
            map["fileServer"] = fileServer
            map["path"] = path
        }
        if (character != "") {
            map["character"] = character
        }

        val array=ArrayList<String>()
        array.add("init")
        array.add("${Gson().toJson(map)}")
        "42"+ Gson().toJson(array)
    }

}