package com.shizq.bika.adapter

import android.view.ViewGroup
import com.shizq.bika.MyApp
import com.shizq.bika.R
import com.shizq.bika.bean.ChatMessage2Bean
import com.shizq.bika.utils.SPUtil
import me.jingbin.library.adapter.BaseByRecyclerViewAdapter
import me.jingbin.library.adapter.BaseByViewHolder

//新聊天室多布局
class ChatMessageMultiAdapter:
    BaseByRecyclerViewAdapter<ChatMessage2Bean, BaseByViewHolder<ChatMessage2Bean>>() {

    val name = SPUtil.get(MyApp.contextBase, "user_name", "") as String

    override fun getItemViewType(position: Int): Int {
        val data = getItemData(position)

        when (data.type) {
            "CONNECTED" -> {
                //连接成功时显示的
                return 1
            }
            "SYSTEM_MESSAGE" -> {
                //系统通知 禁言等
                return 1
            }

            "TEXT_MESSAGE" -> {
                //文字消息
                return if (data.isBlocked) 1 else {
                    if (data.data.profile.name == name) 2 else 3
                }
            }
            "IMAGE_MESSAGE" -> {
                //图片消息
                return if (data.isBlocked) 1 else {
                    if (data.data.profile.name == name) 2 else 3
                }
            }

            else -> {
                return 1
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseByViewHolder<ChatMessage2Bean> {
        return when (viewType) {
            1 -> {
                ChatMessageSystemHolder(parent, R.layout.item_chat_message_system)
            }
            2 -> {
                ChatMessageSendHolder(parent, R.layout.item_chat_message_send)
            }
            else -> {
                ChatMessageReceiveHolder(parent, R.layout.item_chat_message_receive)
            }
        }
    }

}