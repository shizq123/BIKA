package com.shizq.bika.adapter

import android.view.ViewGroup
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ChatMessage2Bean
import com.shizq.bika.databinding.ItemChatMessageSystemBinding
import com.shizq.bika.utils.TimestampFormat

//新聊天室 一些通知
class ChatMessageSystemHolder (viewGroup: ViewGroup, layoutId: Int) :
    BaseBindingHolder<ChatMessage2Bean, ItemChatMessageSystemBinding>(viewGroup,layoutId) {

    override fun onBindingView(
        holder: BaseBindingHolder<*, *>,
        bean: ChatMessage2Bean,
        position: Int
    ) {
        when(bean.type){
            "CONNECTED" -> {
                binding.chatNotification.text = TimestampFormat().getDate(bean.data.data)
            }

            "SYSTEM_MESSAGE" -> {
                binding.chatNotification.text = bean.data.message.message
            }

            "TEXT_MESSAGE" -> {
                binding.chatNotification.text = "黑名单消息"
            }

            "IMAGE_MESSAGE" -> {
                binding.chatNotification.text = "黑名单消息"
            }

            else -> {
                binding.chatNotification.text = "未知消息类型"
            }
        }
    }
}