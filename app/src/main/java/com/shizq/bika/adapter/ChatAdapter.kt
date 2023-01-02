package com.shizq.bika.adapter

import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ChatMessageBean
import com.shizq.bika.databinding.ItemChatBinding
import com.shizq.bika.utils.Base64Util
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.dp
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ChatAdapter :
    BaseBindingAdapter<ChatMessageBean, ItemChatBinding>(R.layout.item_chat) {
    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: ChatMessageBean,
        binding: ItemChatBinding,
        position: Int
    ) {
        //设置一个回复类消息的最小宽度 类似qq

        if (bean.name == null && bean.user_id == null && bean.message != null) {
            //通知 悄悄话
            binding.chatLayoutL.visibility = View.GONE
            binding.chatLayoutR.visibility = View.GONE
            binding.chatNotification.visibility = View.VISIBLE
            binding.chatNotification.text = bean.message
        } else if (bean.type.toString() == "100") {
            //我发送的消息
            binding.chatLayoutL.visibility = View.GONE
            binding.chatLayoutR.visibility = View.VISIBLE
            binding.chatNotification.visibility = View.GONE
            binding.chatNameR.text = bean.name
            binding.chatContentR.text = bean.message
        } else {
            //接收消息
            binding.chatLayoutL.visibility = View.VISIBLE
            binding.chatLayoutR.visibility = View.GONE
            binding.chatNotification.visibility = View.GONE
            binding.chatNameL.text = bean.name
            if (bean.character != null && bean.character != "") {
                GlideApp.with(holder.itemView)
                    .load(bean.character)
                    .into(binding.chatCharacterL)
            }

            //拆分 利于缓存 省流量 加载更快
            GlideApp.with(holder.itemView)
                .load(
                    if (bean.avatar != null && bean.avatar != "") {
                        val i: Int = bean.avatar.indexOf("/static/")
                        if (i > 0)
                            GlideUrlNewKey(
                                bean.avatar.substring(0, i),
                                bean.avatar.substring(i + 8)
                            )
                        else bean.avatar
                    } else R.drawable.placeholder_avatar_2

                )
                .placeholder(R.drawable.placeholder_avatar_2)
                .into(binding.chatAvatarL)

            if (bean.reply_name != null && bean.reply_name != "") {
                binding.chatReplyLayout.visibility = ViewGroup.VISIBLE
                binding.chatReplyName.text = bean.reply_name
                if (bean.reply.length > 50) {
                    //要改 显示两行 尾部显示...
                    binding.chatReply.text = bean.reply.substring(0, 50) + "..."
                } else {
                    binding.chatReply.text = bean.reply
                }
                setReplyLayout(binding.chatReplyLayout)
            } else {
                binding.chatReplyLayout.visibility = ViewGroup.GONE
            }
            if (bean.level >= 0) {
                //等级
                binding.chatLevelL.text = "Lv." + bean.level
            }
            if (bean.at != null && bean.at != "") {
                //艾特某人
                binding.chatAtL.visibility = View.VISIBLE
                binding.chatAtL.text = bean.at.replace("嗶咔_", "@")
            } else {
                binding.chatAtL.visibility = View.GONE
            }

            //显示时间 后面加设置显示隐藏
//            String time = "";
//            if (chatModelList.get(position).getPlatform() != null && !chatModelList.get(position).getPlatform().equals("")) {
//                time = chatModelList.get(position).getPlatform();
//            }
//            time += " " + TimeUtil.getTime();
//            holder.chat_time_l.setText(time);

            if (bean.image != null && bean.image != "") {
                binding.chatContentImageL.visibility = View.VISIBLE
                binding.chatContentL.visibility = View.GONE
                //图片要处理宽高问题
                setImageView(binding.chatContentImageL, Base64Util.base64ToBitmap(bean.image))
            } else {
                binding.chatContentL.visibility = View.VISIBLE
                binding.chatContentImageL.visibility = View.GONE
            }
            if (bean.audio != null && bean.audio != "") {
                //这里要处理语音
                binding.chatContentL.visibility = View.GONE
                binding.chatVoiceL.visibility = View.VISIBLE
            } else {
                binding.chatContentL.visibility = View.VISIBLE
                binding.chatVoiceL.visibility = View.GONE
            }

            if (bean.message != null && bean.message != "") {
                binding.chatContentL.text = bean.message
            }
        }


        holder.addOnClickListener(R.id.chat_avatar_layout_l)
        holder.addOnClickListener(R.id.chat_name_l)
        holder.addOnLongClickListener(R.id.chat_avatar_layout_l)

        val animationDrawable = binding.chatVoiceImageL.background as AnimationDrawable
        binding.chatMessageLayoutL.setOnClickListener {
            if (bean.audio.isNullOrEmpty()) {
                holder.addOnClickListener(R.id.chat_message_layout_l)
            } else {
                binding.chatVoiceDian.visibility = View.GONE
                if (!animationDrawable.isRunning) {
                    playAudio(bean.audio, animationDrawable)
                }
            }
        }
    }

    private fun setImageView(imageView: ImageView, bitmap: Bitmap) {
        //手机截图比例的图片防止占满屏
        val bitmapH = bitmap.height
        val bitmapW = bitmap.width
        val imageMinW = 100.dp
        val layoutParams = imageView.layoutParams
        if (bitmapH > 1.5 * bitmapW) {
            layoutParams.width = imageMinW
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        } else if (bitmapW < 1080) {
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        } else {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        imageView.layoutParams = layoutParams
        imageView.setImageBitmap(bitmap)
    }

    private fun playAudio(audio: String, animationDrawable: AnimationDrawable) {

        val mp3SoundByteArray: ByteArray = Base64.decode(audio.replace("\n", ""), Base64.DEFAULT)

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

    private fun setReplyLayout(view: LinearLayout) {
        //带有回复的消息显示不正确 后面优化

    }
}