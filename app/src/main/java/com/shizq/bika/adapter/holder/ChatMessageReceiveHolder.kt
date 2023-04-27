package com.shizq.bika.adapter.holder

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ChatMessageBean
import com.shizq.bika.databinding.ItemChatMessageReceiveBinding
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.dp
import com.shizq.bika.widget.UserViewDialog

//新聊天室 收到的消息
class ChatMessageReceiveHolder(viewGroup: ViewGroup, layoutId: Int) :
    BaseBindingHolder<ChatMessageBean, ItemChatMessageReceiveBinding>(viewGroup, layoutId) {

    override fun onBindingView(
        holder: BaseBindingHolder<*, *>,
        bean: ChatMessageBean,
        position: Int
    ) {
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
                binding.chatReplyImage.visibility = View.VISIBLE
                GlideApp.with(holder.itemView)
                    .load(reply.image)
                    .placeholder(R.drawable.placeholder_avatar_2)
                    .into(binding.chatReplyImage)
                binding.chatReply.text = "[图片]"
            } else {
                binding.chatReplyImage.visibility = View.GONE
            }
        } else {
            binding.chatReplyLayout.visibility = ViewGroup.GONE
        }

        //消息
        val message = bean.data.message
        //艾特某人
        if (bean.data.userMentions.isNotEmpty()) {
            binding.chatAtGroupL.visibility = View.VISIBLE
            binding.chatAtGroupL.removeAllViews()
            for (i in bean.data.userMentions) {
                val chip = Chip(holder.itemView.context)
                chip.text = i.name
//                chip.textSize = 12f.dp
//                chip.height = 24.dp
                chip.setEnsureMinTouchTargetSize(false)//去除视图的顶部和底部的额外空间
                binding.chatAtGroupL.addView(chip)

                chip.setOnClickListener {
                    UserViewDialog(holder.itemView.context as AppCompatActivity).showUserDialog(i.id)
                }
            }
        } else {
            binding.chatAtGroupL.visibility = View.GONE
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

        holder.addOnClickListener(R.id.chat_avatar_layout_l)
        holder.addOnClickListener(R.id.chat_name_l)
        holder.addOnLongClickListener(R.id.chat_avatar_layout_l)
        holder.addOnClickListener(R.id.chat_message_layout_l)
        holder.addOnClickListener(R.id.chat_reply_layout)
    }
}