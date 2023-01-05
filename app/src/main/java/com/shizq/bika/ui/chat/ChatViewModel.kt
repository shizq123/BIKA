package com.shizq.bika.ui.chat

import android.app.Application
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.shizq.bika.MyApp
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ChatMessageBean
import com.shizq.bika.network.IReceiveMessage
import com.shizq.bika.network.WebSocketManager
import com.shizq.bika.utils.SPUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ChatViewModel(application: Application) : BaseViewModel(application) {
    var url = ""
    lateinit var webSocketManager: WebSocketManager

    var reply: String = ""
    var reply_name: String = ""
    var atname: String = ""

    val liveData_connections: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    val liveData_message: MutableLiveData<ChatMessageBean> by lazy {
        MutableLiveData<ChatMessageBean>()
    }
    val liveData_state: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun WebSocket() {
        webSocketManager.init(url, object : IReceiveMessage {
            override fun onConnectSuccess() {}

            override fun onConnectFailed() {
                liveData_state.postValue("failed")
            }

            override fun onClose() {
                liveData_state.postValue("close")
            }

            override fun onMessage(text: String) {
                Log.d("-----------webSocket---text收到", "" + text)
                if (text == "40") {
                    liveData_state.postValue("success")

                    val array = ArrayList<String>()
                    array.add("init")
                    array.add(Gson().toJson(user()))

                    webSocketManager.sendMessage("42" + Gson().toJson(array))
                }
                if (text.substring(0, 2) == "42") {
                    //收到消息 42 进行解析
                    val key = JsonParser().parse(text.substring(2)).asJsonArray[0].asString
                    val json = JsonParser().parse(text.substring(2)).asJsonArray[1].asJsonObject


                    when (key) {
                        "new_connection" -> {
                            liveData_connections.postValue("${json.get("connections").asString}人在线")
                        }
                        "receive_notification" -> {
                            liveData_message.postValue(
                                Gson().fromJson(
                                    json,
                                    ChatMessageBean::class.java
                                )
                            )
                        }
                        "broadcast_message" -> {
                            liveData_message.postValue(
                                Gson().fromJson(
                                    json,
                                    ChatMessageBean::class.java
                                )
                            )

                        }
                        "broadcast_image" -> {
                            liveData_message.postValue(
                                Gson().fromJson(
                                    json,
                                    ChatMessageBean::class.java
                                )
                            )

                        }
                        "broadcast_audio" -> {
                            liveData_message.postValue(
                                Gson().fromJson(
                                    json,
                                    ChatMessageBean::class.java
                                )
                            )
                        }

                        "connection_close" -> {
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
                        "kick" -> { //提人
                            liveData_message.postValue(
                                Gson().fromJson(
                                    json,
                                    ChatMessageBean::class.java
                                )
                            )
                        }
//

//
//                        "connect"->{ //
//                            liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))
//                        }
                        else -> {
                            //未知类型
                            Log.d("-----------webSocket---text收到", "消息${text.substring(2)}")
//                            liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))

                        }
                    }

                }
            }

        })
    }

    fun sendMessage(text: String) {
        val fileServer = SPUtil.get(MyApp.contextBase, "user_fileServer", "") as String
        val path = SPUtil.get(MyApp.contextBase, "user_path", "") as String
        val character = SPUtil.get(MyApp.contextBase, "user_character", "") as String

        val map = mutableMapOf<String, Any>()
        map["at"] = atname
        map["audio"] = ""
        if (path != "") {
            map["avatar"] = "${fileServer.replace("/static/","")}/static/$path"
        }
        map["block_user_id"] = ""
        if (character != "") {
            map["character"] = character
        }
        map["email"] = SPUtil.get(MyApp.contextBase, "username", "") as String
        map["gender"] = SPUtil.get(MyApp.contextBase, "user_gender", "bot") as String
        map["image"] = ""
        map["level"] = SPUtil.get(MyApp.contextBase, "user_level", 1) as Int
        map["message"] = text
        map["name"] = SPUtil.get(MyApp.contextBase, "user_name", "") as String
        map["reply"] = reply
        map["reply_name"] = reply_name
        map["title"] = SPUtil.get(MyApp.contextBase, "user_title", "") as String
        map["type"] = 3
        map["unique_id"] = ""
        map["user_id"] = SPUtil.get(MyApp.contextBase, "user_id", "") as String
        map["verified"] = SPUtil.get(MyApp.contextBase, "user_verified", false) as Boolean

        val json=Gson().toJson(map)
        val array = ArrayList<String>()
        array.add("send_message")
        array.add(json)

        liveData_message.postValue(Gson().fromJson(json,ChatMessageBean::class.java))
        webSocketManager.sendMessage("42" + Gson().toJson(array))
//        Log.d("---vm---","42" + Gson().toJson(array))
    }

    var user = {
        val fileServer = SPUtil.get(application, "user_fileServer", "")
        val path = SPUtil.get(application, "user_path", "")
        val character = SPUtil.get(application, "user_character", "")

        val map = mutableMapOf(
            "birthday" to SPUtil.get(application, "user_birthday", ""),
            "characters" to ArrayList<Any>(),
            "email" to SPUtil.get(application, "username", ""),
            "exp" to SPUtil.get(application, "user_exp", 0),
            "gender" to SPUtil.get(application, "user_gender", "bot"),
            "isPunched" to SPUtil.get(application, "setting_punch", false),
            "level" to SPUtil.get(application, "user_level", 1),
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

        map
    }

    fun playAudio(audio: String, imageview: View) {
        val voiceImage = imageview as ImageView
        val animationDrawable = voiceImage.background as AnimationDrawable
        if (!animationDrawable.isRunning) {
            // TODO 有bug 会有不播放的情况
            val mp3SoundByteArray: ByteArray =
                Base64.decode(audio.replace("\n", ""), Base64.DEFAULT)

            val tempMp3: File = File.createTempFile("audio", ".mp3")
            val fos = FileOutputStream(tempMp3)
            fos.write(mp3SoundByteArray)
            fos.close()
            val fis = FileInputStream(tempMp3)

            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(fis.fd)
            mediaPlayer.prepareAsync()
            mediaPlayer.isLooping = false

            mediaPlayer.setOnPreparedListener { player ->
                player.start()
                animationDrawable.start()
            }

            mediaPlayer.setOnCompletionListener { mp ->
                mp.stop()
                mp.release()
                tempMp3.delete()
                animationDrawable.stop()
            }
        }
    }
}