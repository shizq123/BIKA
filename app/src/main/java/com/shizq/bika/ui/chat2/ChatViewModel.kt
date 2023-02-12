package com.shizq.bika.ui.chat2

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shizq.bika.MyApp
import com.shizq.bika.base.BaseViewModel
import com.shizq.bika.bean.ChatMessage2Bean
import com.shizq.bika.bean.UserMention
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.websocket.ChatWebSocketManager
import com.shizq.bika.network.websocket.IReceiveMessage
import com.shizq.bika.utils.SPUtil
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import java.io.File
import java.util.*

class ChatViewModel(application: Application) : BaseViewModel(application) {
    var roomId = ""
    lateinit var webSocketManager: ChatWebSocketManager

    var reply: String = ""
    var reply_name: String = ""
    var atname: String = ""

    val liveData_message: MutableLiveData<ChatMessage2Bean> by lazy {
        MutableLiveData<ChatMessage2Bean>()
    }
    val liveData_state: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val liveDataSendMessage: MutableLiveData<ChatMessage2Bean> by lazy {
        MutableLiveData<ChatMessage2Bean>()
    }

    fun WebSocket() {
        val url = RetrofitUtil.LIVE_SERVER +
                "/?token=" +
                SPUtil.get(MyApp.contextBase, "chat_token", "") as String +
                "&room=" + roomId
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
                liveData_state.postValue("success")

                val bean = Gson().fromJson(text, ChatMessage2Bean::class.java)
                if (bean.data != null) {
                    if (bean.type == "TEXT_MESSAGE" || bean.type == "IMAGE_MESSAGE") {
                        //防止重复显示 屏蔽掉自己发送的消息
                        if (bean.data.profile.name !=
                            SPUtil.get(MyApp.contextBase, "user_name", "") as String
                        ) {
                            liveData_message.postValue(bean)
                        }
                    } else {
                        liveData_message.postValue(bean)
                    }
                }

            }

        })
    }

    //发送文字消息 当前不能@不能回复
    fun sendMessage(message: String, userMentions: List<UserMention> = listOf()) {
        val referenceId = UUID.randomUUID().toString()
        //预先添加个数据 等发送成功再进行数据和ui的更新
        liveData_message.postValue(
            Gson().fromJson(
                myMessage(message, "", referenceId, userMentions),
                ChatMessage2Bean::class.java
            )
        )

        val map = mutableMapOf(
            "roomId" to roomId,
            "message" to message,
            "referenceId" to referenceId,
            "userMentions" to userMentions
        )
        val body = RequestBody.create(
            MediaType.parse("application/json; charset=UTF-8"),
            Gson().toJson(map)
        )
        val headers = BaseHeaders().getChatHeaderMapAndToken()
        RetrofitUtil.service_live.ChatSendMessagePost(body, headers)
            .doOnSubscribe(this)
            .subscribe(object : Observer<ChatMessage2Bean> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    var t: ChatMessage2Bean? = null
                    if (e is HttpException) {   //  处理服务器返回的非成功异常
                        val responseBody = e.response()!!.errorBody()
                        if (responseBody != null) {
                            val type = object : TypeToken<ChatMessage2Bean>() {}.type
                            t = Gson().fromJson(responseBody.string(), type)
                            liveDataSendMessage.postValue(t)
                        } else {
                            liveDataSendMessage.postValue(t)
                        }
                    }
                }

                override fun onComplete() {

                }

                override fun onNext(t: ChatMessage2Bean) {
                    liveDataSendMessage.postValue(t)
                }

            })
    }

    //发送图片 当前只能发送单张
    fun sendImage(message: String, path: String, userMentions: List<UserMention> = listOf()) {
        val referenceId = UUID.randomUUID().toString()
        //预先添加个数据 等发送成功再进行数据和ui的更新
        liveData_message.postValue(
            Gson().fromJson(
                myMessage(message, path, referenceId, userMentions),
                ChatMessage2Bean::class.java
            )
        )
        val multipartBody =
            MultipartBody.Builder()
                .setType(MediaType.get("multipart/form-data"))
        multipartBody.addFormDataPart("roomId", roomId)
        multipartBody.addFormDataPart("referenceId", referenceId)

        val captionHeaders = Headers.Builder()
            .addUnsafeNonAscii("content-disposition", "form-data; name=caption")
            .addUnsafeNonAscii("content-transfer-encoding", "binary")
            .build()
        multipartBody.addPart(MultipartBody.Part.create(captionHeaders, RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), message)))

        multipartBody.addFormDataPart("userMentions", userMentions.toString())

        //图片后面可能for循环添加
        val file = File(path)
        val body = RequestBody.create(MediaType.parse("application/octet-stream"), file)
        multipartBody.addFormDataPart("images", file.name, body)

        val multipartBodyBuild = multipartBody.build()
        val headers = BaseHeaders().getChatHeaderMapAndToken()
        headers["content-type"] = "multipart/form-data; boundary=${multipartBodyBuild.boundary()}"

        RetrofitUtil.service_live.ChatSendImagePost(headers, multipartBodyBuild)
            .doOnSubscribe(this@ChatViewModel)
            .subscribe(object : Observer<ChatMessage2Bean> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    var t: ChatMessage2Bean? = null
                    if (e is HttpException) {   //  处理服务器返回的非成功异常
                        val responseBody = e.response()!!.errorBody()
                        if (responseBody != null) {
                            val type = object : TypeToken<ChatMessage2Bean>() {}.type
                            t = Gson().fromJson(responseBody.string(), type)
                            liveDataSendMessage.postValue(t)
                        } else {
                            liveDataSendMessage.postValue(t)
                        }
                    }
                }

                override fun onComplete() {

                }

                override fun onNext(t: ChatMessage2Bean) {
                    liveDataSendMessage.postValue(t)
                }

            })
    }

    //非服务器返回的数据 用于显示ui的数据
    fun myMessage(
        message: String,
        imagePath: String = "",
        referenceId: String,
        userMentions: List<UserMention> = listOf()
    ): String {
        val fileServer = SPUtil.get(MyApp.contextBase, "user_fileServer", "") as String
        val path = SPUtil.get(MyApp.contextBase, "user_path", "") as String
        val avatarUrl =
            if (fileServer == "") "" else "${fileServer.replace("/static/", "")}/static/$path"

        val messageMap = if (imagePath == "") {
            mutableMapOf(
                "message" to message,
                "referenceId" to referenceId,
                "userMentions" to userMentions,
            )
        } else {
            mutableMapOf(
                "caption" to message,
                "medias" to listOf(imagePath),
                "referenceId" to referenceId,
                "userMentions" to userMentions,
            )
        }

        val profile = mutableMapOf(
            "name" to SPUtil.get(MyApp.contextBase, "user_name", "") as String,
            "level" to SPUtil.get(MyApp.contextBase, "user_level", 1) as Int,
            "characters" to listOf<String>(),
            "avatarUrl" to avatarUrl,
        )
        val data = mutableMapOf(
            "message" to messageMap,
            "profile" to profile
        )

        val json = if (imagePath == "") {
            mutableMapOf(
                "type" to "TEXT_MESSAGE",
                "isBlocked" to false,
                "data" to data
            )
        } else {
            mutableMapOf(
                "type" to "IMAGE_MESSAGE",
                "isBlocked" to false,
                "data" to data
            )
        }
        return Gson().toJson(json)
    }

//    //发送文字 可以回复 可以@
//    //发送图片 不可以发送文字 可以回复 可以@
//    //发送语音 不可以发送文字 不可以回复 不可以@
//    //悄悄话 不能发图片和语音
//    fun sendMessage(text: String="",base64Image:String="",base64Audio:String="") {
//        val fileServer = SPUtil.get(MyApp.contextBase, "user_fileServer", "") as String
//        val path = SPUtil.get(MyApp.contextBase, "user_path", "") as String
//        val character = SPUtil.get(MyApp.contextBase, "user_character", "") as String
//
//        val map = mutableMapOf<String, Any>()
//        map["at"] = if (base64Audio=="") atname else ""
//        map["audio"] = base64Audio
//        if (path != "") {
//            map["avatar"] = "${fileServer.replace("/static/", "")}/static/$path"
//        }
//        map["block_user_id"] = ""
//        if (character != "") {
//            map["character"] = character
//        }
//
//        map["email"] = SPUtil.get(MyApp.contextBase, "username", "") as String
//        map["gender"] = SPUtil.get(MyApp.contextBase, "user_gender", "bot") as String
//        map["image"] = base64Image
//        map["level"] = SPUtil.get(MyApp.contextBase, "user_level", 1) as Int
//        map["message"] = text
//        map["name"] = SPUtil.get(MyApp.contextBase, "user_name", "") as String
//        map["platform"] = "android"
//        map["reply"] =if (base64Audio=="") reply else ""
//        map["reply_name"] =if (base64Audio=="")  reply_name else ""
//        map["title"] = SPUtil.get(MyApp.contextBase, "user_title", "") as String
//        map["type"] = if (base64Image!="") 4 else if (base64Audio!="") 5 else 3
//        map["unique_id"] = ""
//        map["user_id"] = SPUtil.get(MyApp.contextBase, "user_id", "") as String
//        map["verified"] = SPUtil.get(MyApp.contextBase, "user_verified", false) as Boolean
//
//        val json = Gson().toJson(map)
//        val array = ArrayList<String>()
//        array.add(if (base64Image!="") "send_image" else if (base64Audio!="") "send_audio" else "send_message")
//        array.add(json)
//        liveData_message.postValue(Gson().fromJson(json, ChatMessageBean::class.java))
//
//        Log.d("------",Gson().toJson(array))
//        webSocketManager.sendMessage("42" + Gson().toJson(array))
//    }
//
//    var init = {
//        val fileServer = SPUtil.get(application, "user_fileServer", "") as String
//        val path = SPUtil.get(application, "user_path", "") as String
//        val character = SPUtil.get(application, "user_character", "") as String
//
//        val map = mutableMapOf<String, Any>()
//
//        if (fileServer != ""&&path!= "") {
//            val avatarMap = mutableMapOf<String, String>()
//            avatarMap["fileServer"] = fileServer
//            avatarMap["originalName"] = "avatar.jpg"
//            avatarMap["path"] = path
//            map["avatar"] = avatarMap
//        }
//        map["birthday"] = SPUtil.get(application, "user_birthday", "") as String
//        if (character != "") {
//            map["character"] = character
//        }
//        map["characters"] = ArrayList<Any>()
//        map["email"] = SPUtil.get(application, "username", "") as String
//        map["exp"] = SPUtil.get(application, "user_exp", 0) as Int
//        map["gender"] = SPUtil.get(application, "user_gender", "bot") as String
//        map["isPunched"] = SPUtil.get(application, "setting_punch", false)
//        map["level"] = SPUtil.get(application, "user_level", 1) as Int
//        map["name"] = SPUtil.get(application, "user_name", "") as String
//        map["slogan"] = SPUtil.get(application, "user_slogan", "") as String
//        map["title"] = SPUtil.get(application, "user_title", "") as String
//        map["_id"] = SPUtil.get(application, "user_id", "") as String
//        map["verified"] = SPUtil.get(application, "user_verified", false) as Boolean
//        map
//    }
//
//    fun playAudio(audio: String, imageview: View) {
//        val voiceImage = imageview as ImageView
//        val animationDrawable = voiceImage.background as AnimationDrawable
//        if (!animationDrawable.isRunning) {
//            // TODO 有bug 会有不播放的情况
//            val mp3SoundByteArray: ByteArray =
//                Base64.decode(audio.replace("\n", ""), Base64.DEFAULT)
//
//            val tempMp3: File = File.createTempFile("audio", ".mp3")
//            val fos = FileOutputStream(tempMp3)
//            fos.write(mp3SoundByteArray)
//            fos.close()
//            val fis = FileInputStream(tempMp3)
//
//            val mediaPlayer = MediaPlayer()
//            mediaPlayer.setDataSource(fis.fd)
//            mediaPlayer.prepareAsync()
//            mediaPlayer.isLooping = false
//
//            mediaPlayer.setOnPreparedListener { player ->
//                player.start()
//                animationDrawable.start()
//            }
//
//            mediaPlayer.setOnCompletionListener { mp ->
//                mp.stop()
//                mp.release()
//                tempMp3.delete()
//                animationDrawable.stop()
//            }
//        }
//    }
}