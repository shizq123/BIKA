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
import com.google.gson.JsonParser.parseString
import com.shizq.bika.MyApp
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ChatMessageBean
import com.shizq.bika.network.websocket.IReceiveMessage
import com.shizq.bika.network.websocket.WebSocketManager
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
        webSocketManager.init(url, object :
            IReceiveMessage {
            override fun onConnectSuccess() {}

            override fun onConnectFailed() {
                liveData_state.postValue("failed")
            }

            override fun onClose() {
                liveData_state.postValue("close")
            }

            override fun onMessage(text: String) {
                if (text == "40") {
                    liveData_state.postValue("success")

                    val array = ArrayList<String>()
                    array.add("init")
                    array.add(Gson().toJson(init()))

                    webSocketManager.sendMessage("42" + Gson().toJson(array))
                }
                if (text.substring(0, 2) == "42") {
                    //收到消息 42 进行解析
                    val key = parseString(text.substring(2)).asJsonArray[0].asString
                    val json = parseString(text.substring(2)).asJsonArray[1].asJsonObject

                    when (key) {
                        //broadcast_ads是广告
                        "broadcast_ads" -> {}
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

                        else -> {
                            //未知类型
                            liveData_message.postValue(
                                Gson().fromJson(
                                    json,
                                    ChatMessageBean::class.java
                                )
                            )

                        }
                    }

                }
            }

        })
    }

    //发送文字 可以回复 可以@
    //发送图片 不可以发送文字 可以回复 可以@
    //发送语音 不可以发送文字 不可以回复 不可以@
    //悄悄话 不能发图片和语音
    fun sendMessage(text: String="",base64Image:String="",base64Audio:String="") {
        val fileServer = SPUtil.get(MyApp.contextBase, "user_fileServer", "") as String
        val path = SPUtil.get(MyApp.contextBase, "user_path", "") as String
        val character = SPUtil.get(MyApp.contextBase, "user_character", "") as String

        val map = mutableMapOf<String, Any>()
        map["at"] = if (base64Audio=="") atname else ""
        map["audio"] = base64Audio
        if (path != "") {
            map["avatar"] = "${fileServer.replace("/static/", "")}/static/$path"
        }
        map["block_user_id"] = ""
        if (character != "") {
            map["character"] = character
        }

        map["email"] = SPUtil.get(MyApp.contextBase, "username", "") as String
        map["gender"] = SPUtil.get(MyApp.contextBase, "user_gender", "bot") as String
        map["image"] = base64Image
        map["level"] = SPUtil.get(MyApp.contextBase, "user_level", 1) as Int
        map["message"] = text
        map["name"] = SPUtil.get(MyApp.contextBase, "user_name", "") as String
        map["platform"] = "android"
        map["reply"] =if (base64Audio=="") reply else ""
        map["reply_name"] =if (base64Audio=="")  reply_name else ""
        map["title"] = SPUtil.get(MyApp.contextBase, "user_title", "") as String
        map["type"] = if (base64Image!="") 4 else if (base64Audio!="") 5 else 3
        map["unique_id"] = ""
        map["user_id"] = SPUtil.get(MyApp.contextBase, "user_id", "") as String
        map["verified"] = SPUtil.get(MyApp.contextBase, "user_verified", false) as Boolean

        val json = Gson().toJson(map)
        val array = ArrayList<String>()
        array.add(if (base64Image!="") "send_image" else if (base64Audio!="") "send_audio" else "send_message")
        array.add(json)
        liveData_message.postValue(Gson().fromJson(json, ChatMessageBean::class.java))

        Log.d("------",Gson().toJson(array))
        webSocketManager.sendMessage("42" + Gson().toJson(array))
    }

    var init = {
        val fileServer = SPUtil.get(application, "user_fileServer", "") as String
        val path = SPUtil.get(application, "user_path", "") as String
        val character = SPUtil.get(application, "user_character", "") as String

        val map = mutableMapOf<String, Any>()

        if (fileServer != ""&&path!= "") {
            val avatarMap = mutableMapOf<String, String>()
            avatarMap["fileServer"] = fileServer
            avatarMap["originalName"] = "avatar.jpg"
            avatarMap["path"] = path
            map["avatar"] = avatarMap
        }
        map["birthday"] = SPUtil.get(application, "user_birthday", "") as String
        if (character != "") {
            map["character"] = character
        }
        map["characters"] = ArrayList<Any>()
        map["email"] = SPUtil.get(application, "username", "") as String
        map["exp"] = SPUtil.get(application, "user_exp", 0) as Int
        map["gender"] = SPUtil.get(application, "user_gender", "bot") as String
        map["isPunched"] = SPUtil.get(application, "setting_punch", false)
        map["level"] = SPUtil.get(application, "user_level", 1) as Int
        map["name"] = SPUtil.get(application, "user_name", "") as String
        map["slogan"] = SPUtil.get(application, "user_slogan", "") as String
        map["title"] = SPUtil.get(application, "user_title", "") as String
        map["_id"] = SPUtil.get(application, "user_id", "") as String
        map["verified"] = SPUtil.get(application, "user_verified", false) as Boolean
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