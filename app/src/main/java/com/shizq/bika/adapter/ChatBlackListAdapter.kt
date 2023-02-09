package com.shizq.bika.adapter

import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ChatBlackListBean
import com.shizq.bika.databinding.ItemChatBlacklistBinding
import com.shizq.bika.utils.GlideApp

class ChatBlackListAdapter :
    BaseBindingAdapter<ChatBlackListBean.Doc, ItemChatBlacklistBinding>(R.layout.item_chat_blacklist) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: ChatBlackListBean.Doc,
        binding: ItemChatBlacklistBinding,
        position: Int
    ) {

        GlideApp.with(holder.itemView)
            .load(bean.user.avatarUrl)
            .placeholder(R.drawable.placeholder_avatar_2)
            .into(binding.chatBlacklistAvatar)

        holder.addOnClickListener(R.id.chat_blacklist_delete)

    }
}