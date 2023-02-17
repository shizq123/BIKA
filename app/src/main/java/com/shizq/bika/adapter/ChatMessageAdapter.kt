package com.shizq.bika.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
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
import com.shizq.bika.utils.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

//新聊天室 聊天页面
class ChatMessageAdapter :
    BaseBindingAdapter<ChatMessage2Bean, ItemChatBinding>(R.layout.item_chat) {
    val name = SPUtil.get(MyApp.contextBase, "user_name", "") as String
    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: ChatMessage2Bean,
        binding: ItemChatBinding,
        position: Int
    ) {
        if (bean.type == "CONNECTED") {
            //连接成功第一条消息
            binding.chatLayoutL.visibility = View.GONE
            binding.chatLayoutR.visibility = View.GONE
            binding.chatNotification.visibility = View.VISIBLE
            binding.chatNotification.text = TimestampFormat().getDate(bean.data.data)
        } else if (bean.type == "SYSTEM_MESSAGE") {
            //{"type":"SYSTEM_MESSAGE","isBlocked":false,"data":{"action":"MUTE_USER","message":{"id":"hkGF2VNuneCeXxgf-aXrc","referenceId":"_WTa5ewBKkROW0OQYS775","message":"lassassino被禁言1土豆年","date":"2023-02-09T12:02:20+00:00"}}}
            //系统通知（禁言）
            binding.chatLayoutL.visibility = View.GONE
            binding.chatLayoutR.visibility = View.GONE
            binding.chatNotification.visibility = View.VISIBLE
            binding.chatNotification.text = bean.data.message.message
        } else if (bean.type == "TEXT_MESSAGE" || bean.type == "IMAGE_MESSAGE") {
            //接收消息（文字，图片）
            val profile = bean.data.profile
            if (profile.name == name) {
                //自己发的消息
                //根据名字判断 或者后面改成判断用户id
                binding.chatLayoutL.visibility = View.GONE
                binding.chatLayoutR.visibility = View.VISIBLE
                binding.chatNotification.visibility = View.GONE
                binding.chatNameR.text = profile.name

                if (profile.id.isNullOrEmpty()) {
                    binding.chatMessageProgress.visibility = View.VISIBLE
                } else {
                    binding.chatMessageProgress.visibility = View.GONE
                }

                //头像
                GlideApp.with(holder.itemView)
                    .load(
                        if (profile.avatarUrl != null && profile.avatarUrl != "") {
                            val i: Int = profile.avatarUrl.indexOf("/static/")
                            if (i > 0){
                                GlideUrlNewKey(
                                    profile.avatarUrl.substring(0, i),
                                    profile.avatarUrl.substring(i + 8)
                                )}
                            else profile.avatarUrl
                        } else R.drawable.placeholder_avatar_2
                    )
                    .placeholder(R.drawable.placeholder_avatar_2)
                    .into(binding.chatAvatarR)

                //等级
                if (profile.level >= 0) { binding.chatLevelR.text = "Lv." + profile.level }

                //管理员 骑士等 图标和哔咔聊天室显示一致
                binding.chatKnightR.visibility = View.GONE
                binding.chatOfficialR.visibility = View.GONE
                binding.chatManagerR.visibility = View.GONE
                if (profile.characters.isNotEmpty()) {
                    for (i in profile.characters) {
                        when (i) {
                            "knight" -> {
                                binding.chatKnightR.visibility = View.VISIBLE
                            }
                            "official" -> {
                                binding.chatOfficialR.visibility = View.VISIBLE

                            }
                            "manager" -> {
                                binding.chatManagerR.visibility = View.VISIBLE

                            }
                        }

                    }
                }

                //回复的信息
                val reply = bean.data.reply
                if (reply != null) {
                    binding.chatReplyLayoutR.visibility = ViewGroup.VISIBLE
                    binding.chatReplyNameR.text = reply.name
                    if (reply.type == "TEXT_MESSAGE") {
                        binding.chatReplyR.text = reply.message
                    }
                    if (reply.type == "IMAGE_MESSAGE") {
                        binding.chatReplyR.text = "[图片]"
                    }
                } else {
                    binding.chatReplyLayoutR.visibility = ViewGroup.GONE
                }

                //消息
                val message = bean.data.message
                if (message.userMentions.isNotEmpty()) {
                    //艾特某人
                    binding.chatAtR.visibility = View.VISIBLE
                    var name = ""
                    for (i in message.userMentions) {
                        name = "@" + i.name
                    }
                    binding.chatAtR.text = name
                } else {
                    binding.chatAtR.visibility = View.GONE
                }

                if (bean.type == "TEXT_MESSAGE") {
                    binding.chatContentR.visibility = View.VISIBLE
                    binding.chatContentR.text = message.message
                } else {
                    binding.chatContentR.visibility = View.GONE
                }

                if (bean.type == "IMAGE_MESSAGE") {
                    binding.chatContentImageR.visibility = View.VISIBLE
                    if (message.caption != null && message.caption != "") {
                        binding.chatContentR.visibility = View.VISIBLE
                        binding.chatContentR.text = message.caption
                    } else {
                        binding.chatContentR.visibility = View.GONE
                    }
                    GlideApp.with(holder.itemView)
                        .load(message.medias[0])
                        .placeholder(R.drawable.placeholder_avatar_2)
                        .into(binding.chatContentImageR)
                } else {
                    binding.chatContentImageR.visibility = View.GONE
                }
            } else {
                if (bean.isBlocked) {
                    //黑名单
                    binding.chatLayoutL.visibility = View.GONE
                    binding.chatLayoutR.visibility = View.GONE
                    binding.chatNotification.visibility = View.VISIBLE
                    binding.chatNotification.text = "黑名单消息"
                } else {
                    binding.chatLayoutL.visibility = View.VISIBLE
                    binding.chatLayoutR.visibility = View.GONE
                    binding.chatNotification.visibility = View.GONE
                    //发送者信息
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

                    //图标和哔咔聊天室显示一致
                    binding.chatKnight.visibility = View.GONE
                    binding.chatOfficial.visibility = View.GONE
                    binding.chatManager.visibility = View.GONE
                    if (profile.characters.isNotEmpty()) {
                        for (i in profile.characters) {
                            when (i) {
                                "knight" -> {
                                    binding.chatKnight.visibility = View.VISIBLE
                                }
                                "official" -> {
                                    binding.chatOfficial.visibility = View.VISIBLE

                                }
                                "manager" -> {
                                    binding.chatManager.visibility = View.VISIBLE

                                }
                            }

                        }
                    }
                    //回复的信息
                    val reply = bean.data.reply
                    if (reply != null) {
                        binding.chatReplyLayout.visibility = ViewGroup.VISIBLE
                        binding.chatReplyName.text = reply.name
                        if (reply.type == "TEXT_MESSAGE") {
                            binding.chatReply.text = reply.message
                        }
                        if (reply.type == "IMAGE_MESSAGE") {
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
                        var name = ""
                        for (i in message.userMentions) {
                            name = "@" + i.name
                        }
                        binding.chatAtL.text = name
                    } else {
                        binding.chatAtL.visibility = View.GONE
                    }

                    if (bean.type == "TEXT_MESSAGE") {
                        binding.chatContentL.visibility = View.VISIBLE
                        binding.chatContentL.text = message.message
                    } else {
                        binding.chatContentL.visibility = View.GONE
                    }

                    if (bean.type == "IMAGE_MESSAGE") {
                        binding.chatContentImageL.visibility = View.VISIBLE
                        if (message.caption != null && message.caption != "") {
                            binding.chatContentL.visibility = View.VISIBLE
                            binding.chatContentL.text = message.caption
                        } else {
                            binding.chatContentL.visibility = View.GONE
                        }
                        GlideApp.with(holder.itemView)
                            .load(message.medias[0])
                            .placeholder(R.drawable.placeholder_avatar_2)
//                            .placeholder(binding.chatContentImageL.drawable as BitmapDrawable) //并不能用已经加载的图片做占位图
                            .into(binding.chatContentImageL)
                    } else {
                        binding.chatContentImageL.visibility = View.GONE
                    }

                }
            }
        } else {
            binding.chatLayoutL.visibility = View.GONE
            binding.chatLayoutR.visibility = View.GONE
            binding.chatNotification.visibility = View.VISIBLE
            binding.chatNotification.text = "未知消息类型"
        }

        holder.addOnClickListener(R.id.chat_avatar_layout_l)
        holder.addOnClickListener(R.id.chat_name_l)
        holder.addOnLongClickListener(R.id.chat_avatar_layout_l)
        holder.addOnClickListener(R.id.chat_message_layout_l)
        holder.addOnClickListener(R.id.chat_message_layout_r)
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