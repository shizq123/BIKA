package com.shizq.bika.adapter.holder

import android.view.ViewGroup
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ChatMessageBean
import com.shizq.bika.databinding.ItemChatMessageSystemBinding
import com.shizq.bika.utils.TimeUtil

//新聊天室 一些通知
class ChatMessageSystemHolder(viewGroup: ViewGroup, layoutId: Int) :
    BaseBindingHolder<ChatMessageBean, ItemChatMessageSystemBinding>(viewGroup, layoutId) {

    override fun onBindingView(
        holder: BaseBindingHolder<*, *>,
        bean: ChatMessageBean,
        position: Int
    ) {
        binding.chatNotification.text = when (bean.type) {
            "CONNECTED" -> {
                TimeUtil().getDate(bean.data.data)
            }

            "SYSTEM_MESSAGE" -> {
                bean.data.message.message
            }

            "TEXT_MESSAGE" -> {
                "黑名单消息"
            }

            "IMAGE_MESSAGE" -> {
                "黑名单消息"
            }

            else -> {
                "未知消息类型"
            }
        }
    }
}