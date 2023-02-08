package com.shizq.bika.adapter

import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ChatRoomListBean
import com.shizq.bika.databinding.ItemChatRoomsBinding
import com.shizq.bika.utils.GlideApp

class ChatRoomsAdapter :
    BaseBindingAdapter<ChatRoomListBean.Room, ItemChatRoomsBinding>(R.layout.item_chat_rooms) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: ChatRoomListBean.Room,
        binding: ItemChatRoomsBinding,
        position: Int
    ) {

        GlideApp.with(holder.itemView)
            .load(bean.icon)
            .placeholder(R.drawable.placeholder_avatar_2)
            .into(binding.chatRoomsImage)

    }
}