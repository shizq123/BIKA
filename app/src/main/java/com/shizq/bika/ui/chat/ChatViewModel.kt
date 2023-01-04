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
    val liveData_state: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun WebSocket(){
        webSocketManager.init(url,object : IReceiveMessage {
            override fun onConnectSuccess() {}

            override fun onConnectFailed() {
                liveData_state.postValue("failed")
            }

            override fun onClose() {
                liveData_state.postValue("close")
            }

            override fun onMessage(text: String) {
                Log.d("-----------webSocket---text收到",""+text)
                if (text=="40"){
                    liveData_state.postValue("success")
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

                        "connection_close"->{
                            liveData_connections.postValue("${json.get("connections").asString}人在线")
                        }

//                        "set_profile"->{ //
//                            liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))
//                        }
//
//                        "got_private_message"->{ //悄悄话
//                            liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))
//                        }
//
//                        "change_character_icon"->{ //头像框 相关消息
//                            liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))
//                        }
//
//                        "change_title"->{ // 个人title 相关消息
//                            liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))
//                        }
//
                        "kick"->{ //提人
                            liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))
                        }
//

//
//                        "connect"->{ //
//                            liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))
//                        }
                        else ->{
                            Log.d("-----------webSocket---text收到","消息${text.substring(2)}")

                        }
                    }

                }
            }

        })
    }

    //42["send_message","{\"at\":\"\",\"audio\":\"\",\"block_user_id\":\"\",\"character\":\"https:\/\/bidobido.xyz\/special\/frame-632.png\",\"email\":\"shizqhh1\",\"gender\":\"bot\",\"image\":\"\",\"level\":1,\"message\":\"🍵\",\"name\":\"why?why\",\"platform\":\"android\",\"reply\":\"\",\"reply_name\":\"\",\"title\":\"萌新\",\"type\":3,\"unique_id\":\"\",\"user_id\":\"63abf445ed45ccddf959a103\",\"verified\":false}"]
    //42["send_message","{\"at\":\"\",\"audio\":\"\",\"block_user_id\":\"\",\"image\":\"\",\"message\":\"🍵\",\"platform\":\"android\",\"reply\":\"\",\"reply_name\":\"\",\"type\":3,\"unique_id\":\"\",}"]
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