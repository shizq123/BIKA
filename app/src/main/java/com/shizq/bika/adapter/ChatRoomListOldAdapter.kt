package com.shizq.bika.adapter

import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ChatRoomListOldBean
import com.shizq.bika.databinding.ItemChatRoomListOldBinding
import com.shizq.bika.utils.GlideApp

//旧聊天室列表
class ChatRoomListOldAdapter :
    BaseBindingAdapter<ChatRoomListOldBean.Chat, ItemChatRoomListOldBinding>(R.layout.item_chat_room_list_old) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: ChatRoomListOldBean.Chat,
        binding: ItemChatRoomListOldBinding,
        position: Int
    ) {

        GlideApp.with(holder.itemView)
            .load(bean.avatar)
            .placeholder(R.drawable.placeholder_avatar_2)
            .into(binding.chatListImage)

    }
}