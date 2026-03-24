package com.shizq.bika.adapter

//(R.layout.item_notifications)
class NotificationsAdapter {
//    override fun bindView(
//        holder: BaseBindingHolder<*, *>,
//        bean: NotificationsBean.Notifications.Doc,
//        binding: ItemNotificationsBinding,
//        position: Int
//    ) {
//        binding.itemNotificationsTime.text=TimeUtil().time(bean.created_at)
//        if (bean._sender.avatar != null) {//头像
//            Glide.with(holder.itemView)
//                .load(
//                    GlideUrlNewKey(
//                        bean._sender.avatar.fileServer,
//                        bean._sender.avatar.path
//                    )
//                )
//                .centerCrop()
//                .placeholder(R.drawable.placeholder_avatar_2)
//                .into(binding.itemNotificationsUserImage)
//        }
//        if (bean._sender.character != null) { //头像框 新用户没有
//            Glide.with(holder.itemView)
//                .load(bean._sender.character)
//                .into(binding.itemNotificationsUserCharacter)
//        }
//        if (bean.cover != null) { //头像框 新用户没有
//            binding.itemNotificationsCover.visibility = View.VISIBLE
//            Glide.with(holder.itemView)
//                .load(
//                    GlideUrlNewKey(
//                        bean.cover.fileServer,
//                        bean.cover.path
//                    )
//                )
//                .into(binding.itemNotificationsCover)
//        } else {
//            binding.itemNotificationsCover.visibility = View.GONE
//        }
//
//        holder.addOnClickListener(R.id.item_notifications_image_layout)
//    }
}