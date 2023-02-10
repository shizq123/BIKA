package com.shizq.bika.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.shizq.bika.MyApp
import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ChatMessage2Bean
import com.shizq.bika.databinding.ItemChatBinding
import com.shizq.bika.utils.Base64Util
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.SPUtil
import com.shizq.bika.utils.dp
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

//新聊天室 聊天页面
class ChatMessageAdapter :
    BaseBindingAdapter<ChatMessage2Bean, ItemChatBinding>(R.layout.item_chat) {
    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: ChatMessage2Bean,
        binding: ItemChatBinding,
        position: Int
    ) {
        //{"type":"SYSTEM_MESSAGE","isBlocked":false,"data":{"action":"MUTE_USER","message":{"id":"hkGF2VNuneCeXxgf-aXrc","referenceId":"_WTa5ewBKkROW0OQYS775","message":"lassassino被禁言1土豆年","date":"2023-02-09T12:02:20+00:00"}}}
        if (bean.type=="CONNECTED") {
            //通知
            binding.chatLayoutL.visibility = View.GONE
            binding.chatLayoutR.visibility = View.GONE
            binding.chatNotification.visibility = View.VISIBLE
            binding.chatNotification.text = bean.data.data
        }else if (bean.type=="SYSTEM_MESSAGE") {
            //通知
            binding.chatLayoutL.visibility = View.GONE
            binding.chatLayoutR.visibility = View.GONE
            binding.chatNotification.visibility = View.VISIBLE
            binding.chatNotification.text = bean.data.message.message
        }
//        else if (bean.name != null && bean.name == SPUtil.get(
//                MyApp.contextBase,
//                "user_name",
//                ""
//            ) as String
//        ) {
//            //我发送的消息
//            binding.chatLayoutL.visibility = View.GONE
//            binding.chatLayoutR.visibility = View.VISIBLE
//            binding.chatNotification.visibility = View.GONE
//            binding.chatNameR.text = bean.name
//
//            //头像框
//            GlideApp.with(holder.itemView)
//                .load(if (bean.character != null && bean.character != "") bean.character else "")
//                .into(binding.chatCharacterR)
//
//            //头像
//            GlideApp.with(holder.itemView)
//                .load(
//                    if (bean.avatar != null && bean.avatar != "") {
//                        val i: Int = bean.avatar.indexOf("/static/")
//                        if (i > 0)
//                            GlideUrlNewKey(
//                                bean.avatar.substring(0, i),
//                                bean.avatar.substring(i + 8)
//                            )
//                        else bean.avatar
//                    } else R.drawable.placeholder_avatar_2
//
//                )
//                .placeholder(R.drawable.placeholder_avatar_2)
//                .into(binding.chatAvatarR)
//            if (bean.reply_name != null && bean.reply_name != "") {
//                binding.chatReplyLayoutR.visibility = ViewGroup.VISIBLE
//                binding.chatReplyNameR.text = bean.reply_name
//                if (bean.reply.length > 50) {
//                    //要改 TODO  显示两行 尾部显示...
//                    binding.chatReplyR.text = bean.reply.substring(0, 50) + "..."
//                } else {
//                    binding.chatReplyR.text = bean.reply
//                }
//
//            } else {
//                binding.chatReplyLayoutR.visibility = ViewGroup.GONE
//            }
//
//            if (bean.level >= 0) {
//                //等级
//                binding.chatLevelR.text = "Lv." + bean.level
//            }
//
//            binding.chatVerifiedR.visibility = if (bean.verified) View.VISIBLE else View.GONE
//
//            if (bean.at != null && bean.at != "") {
//                //艾特某人
//                binding.chatAtR.visibility = View.VISIBLE
//                binding.chatAtR.text = bean.at.replace("嗶咔_", "@")
//            } else {
//                binding.chatAtR.visibility = View.GONE
//            }
//
//            //显示时间 后面加设置显示隐藏
////            String time = "";
////            if (chatModelList.get(position).getPlatform() != null && !chatModelList.get(position).getPlatform().equals("")) {
////                time = chatModelList.get(position).getPlatform();
////            }
////            time += " " + TimeUtil.getTime();
////            holder.chat_time_r.setText(time);
//
//            if (bean.image != null && bean.image != "") {
//                binding.chatContentImageR.visibility = View.VISIBLE
//                //图片要处理宽高问题
//                setImageView(binding.chatContentImageR, Base64Util().base64ToBitmap(bean.image))
//            } else {
//                binding.chatContentImageR.visibility = View.GONE
//            }
//            if (bean.audio != null && bean.audio != "") {
//                //这里要处理语音
//                binding.chatVoiceR.visibility = View.VISIBLE
//            } else {
//                binding.chatVoiceR.visibility = View.GONE
//            }
//
//            if (bean.message != null && bean.message != "") {
//                binding.chatContentR.visibility = View.VISIBLE
//                if (bean.event_colors != null && bean.event_colors.isNotEmpty()) {
//                    setTextViewStyles(binding.chatContentR, bean.event_colors, bean.message)
//
//                } else {
//                    binding.chatContentR.text = bean.message
//
//                }
//            } else {
//                binding.chatContentR.visibility = View.GONE
//            }
//
//        }
        else if(bean.type=="TEXT_MESSAGE"||bean.type=="IMAGE_MESSAGE"){
            //接收消息
            if (bean.isBlocked){
                //黑名单
                binding.chatLayoutL.visibility = View.GONE
                binding.chatLayoutR.visibility = View.GONE
                binding.chatNotification.visibility = View.VISIBLE
                binding.chatNotification.text = "黑名单消息"
            }else{
                binding.chatLayoutL.visibility = View.VISIBLE
                binding.chatLayoutR.visibility = View.GONE
                binding.chatNotification.visibility = View.GONE
                //发送者信息
                val profile = bean.data.profile
                binding.chatNameL.text = profile.name

                //头像 //拆分 利于缓存 省流量 加载更快
                GlideApp.with(holder.itemView)
                    .load(
                        if (profile.avatarUrl != null && profile.avatarUrl != "") {
                            val i: Int = profile.avatarUrl.indexOf("/static/")
                            if (i > 0) {
                                GlideUrlNewKey(
                                    profile.avatarUrl.substring(0, i),
                                    profile.avatarUrl.substring(i + 8)
                                )
                            } else profile.avatarUrl
                        } else R.drawable.placeholder_avatar_2

                    )
                    .placeholder(R.drawable.placeholder_avatar_2)
                    .into(binding.chatAvatarL)

                if (profile.level >= 0) {
                    //等级
                    binding.chatLevelL.text = "Lv." + profile.level
                }

                //和哔咔聊天室显示一致
                if (profile.characters.isNotEmpty()) {
                    for (i in profile.characters) {
                        when(i){
                            "knight" -> {
                                binding.chatKnight.visibility=View.VISIBLE
                            }
                            "official" -> {
                                binding.chatOfficial.visibility=View.VISIBLE

                            }
                            "manager" -> {
                                binding.chatManager.visibility=View.VISIBLE

                            }
                        }

                    }
                }
                //回复的信息
                val reply = bean.data.reply
                if (reply != null) {
                    binding.chatReplyLayout.visibility = ViewGroup.VISIBLE
                    binding.chatReplyName.text = reply.name
                    if (reply.type=="TEXT_MESSAGE"){
                        if (reply.message.length > 50) {
                            //要改 TODO  显示两行 尾部显示...
                            binding.chatReply.text = reply.message.substring(0, 50) + "..."
                        } else {
                            binding.chatReply.text = reply.message
                        }
                    }

                    if (reply.type=="IMAGE_MESSAGE"){
                       binding.chatReply.text = "[图片]"
                    }


                } else {
                    binding.chatReplyLayout.visibility = ViewGroup.GONE
                }

                //消息
                val message = bean.data.message
                if (message.userMentions.isNotEmpty()) {
                    //艾特某人
                    binding.chatAtL.visibility = View.VISIBLE
                    var name=""
                    for (i in message.userMentions){
                        name="@"+i.name
                    }
                    binding.chatAtL.text=name
                } else {
                    binding.chatAtL.visibility = View.GONE
                }

                if (bean.type=="TEXT_MESSAGE"){
                    binding.chatContentL.visibility = View.VISIBLE
                    binding.chatContentL.text = message.message
                }else{
                    binding.chatContentL.visibility = View.GONE
                }

                if (bean.type=="IMAGE_MESSAGE"){
                    binding.chatContentImageL.visibility = View.VISIBLE
                    if (message.caption != null && message.caption != "") {
                        binding.chatContentL.visibility = View.VISIBLE
                        binding.chatContentL.text = message.caption
                    } else {
                        binding.chatContentL.visibility = View.GONE
                    }
                    GlideApp.with(holder.itemView)
                        .load(message.medias[0])
                        .placeholder(R.drawable.placeholder_transparent_low)
                        .into(binding.chatContentImageL)
                }else{
                    binding.chatContentImageL.visibility = View.GONE
                }

            }

        }else{
            binding.chatLayoutL.visibility = View.GONE
            binding.chatLayoutR.visibility = View.GONE
            binding.chatNotification.visibility = View.VISIBLE
            binding.chatNotification.text = "未知消息类型"
        }

//        holder.addOnClickListener(R.id.chat_avatar_layout_l)
//        holder.addOnClickListener(R.id.chat_name_l)
//        holder.addOnLongClickListener(R.id.chat_avatar_layout_l)
//        holder.addOnClickListener(R.id.chat_message_layout_l)
//        holder.addOnClickListener(R.id.chat_message_layout_r)
    }

    private fun setImageView(imageView: ImageView, bitmap: Bitmap) {
        //手机截图比例的图片防止占满屏
        val bitmapH = bitmap.height
        val bitmapW = bitmap.width
        val imageMinW = 150.dp
        val layoutParams = imageView.layoutParams
        if (bitmapH > 1.7 * bitmapW) {
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
}