package com.shizq.bika.adapter

import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ChatListBean
import com.shizq.bika.databinding.ItemChatlistBinding
import com.shizq.bika.utils.GlideApp

class ChatListAdapter :
    BaseBindingAdapter<ChatListBean.Chat, ItemChatlistBinding>(R.layout.item_chatlist) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: ChatListBean.Chat,
        binding: ItemChatlistBinding,
        position: Int
    ) {

        GlideApp.with(holder.itemView)
            .load(bean.avatar)
            .placeholder(R.drawable.placeholder_avatar_2)
            .into(binding.chatListImage)

    }
}