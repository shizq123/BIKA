package com.shizq.bika.ui.chat

import android.app.Application
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.util.Base64
import android.view.View
import android.widget.ImageView
import com.shizq.bika.base.BaseViewModel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ChatViewModel(application: Application) : BaseViewModel(application) {
    var url = ""

    var reply: String = ""
    var reply_name: String = ""
    var atname: String = ""

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