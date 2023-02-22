package com.shizq.bika.adapter

import android.view.View
import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.NotificationsBean
import com.shizq.bika.databinding.ItemNotificationsBinding
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.TimeUtil

class NotificationsAdapter :
    BaseBindingAdapter<NotificationsBean.Notifications.Doc, ItemNotificationsBinding>(R.layout.item_notifications) {
    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: NotificationsBean.Notifications.Doc,
        binding: ItemNotificationsBinding,
        position: Int
    ) {
        binding.itemNotificationsTime.text=TimeUtil().time(bean.created_at)
        if (bean._sender.avatar != null) {//头像
            GlideApp.with(holder.itemView)
                .load(
                    GlideUrlNewKey(
                        bean._sender.avatar.fileServer,
                        bean._sender.avatar.path
                    )
                )
                .centerCrop()
                .placeholder(R.drawable.placeholder_avatar_2)
                .into(binding.itemNotificationsUserImage)
        }
        if (bean._sender.character != null) { //头像框 新用户没有
            GlideApp.with(holder.itemView)
                .load(bean._sender.character)
                .into(binding.itemNotificationsUserCharacter)
        }
        if (bean.cover != null) { //头像框 新用户没有
            binding.itemNotificationsCover.visibility = View.VISIBLE
            GlideApp.with(holder.itemView)
                .load(
                    GlideUrlNewKey(
                        bean.cover.fileServer,
                        bean.cover.path
                    )
                )
                .into(binding.itemNotificationsCover)
        } else {
            binding.itemNotificationsCover.visibility = View.GONE
        }

        holder.addOnClickListener(R.id.item_notifications_image_layout)
    }
}