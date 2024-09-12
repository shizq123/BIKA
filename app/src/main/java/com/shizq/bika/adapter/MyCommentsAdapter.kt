package com.shizq.bika.adapter

import android.view.ViewGroup
import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.MyCommentsBean
import com.shizq.bika.databinding.ItemMyCommentsBinding
import com.bumptech.glide.Glide
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.SPUtil
import com.shizq.bika.utils.TimeUtil

class MyCommentsAdapter: BaseBindingAdapter<MyCommentsBean.Comments.Doc, ItemMyCommentsBinding>(R.layout.item_my_comments) {

    var fileServer = ""
    var path = ""
    var character = ""
    var name = ""
    var gender = ""
    var level = 1

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseBindingHolder<MyCommentsBean.Comments.Doc, ItemMyCommentsBinding> {
        fileServer = SPUtil.get("user_fileServer", "") as String
        path = SPUtil.get("user_path", "") as String
        character = SPUtil.get("user_character", "") as String
        name = SPUtil.get("user_name", "") as String
        gender = SPUtil.get("user_gender", "") as String
        level = SPUtil.get("user_level", 1) as Int

        return super.onCreateViewHolder(parent, viewType)
    }
    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: MyCommentsBean.Comments.Doc,
        binding: ItemMyCommentsBinding,
        position: Int
    ) {
        binding.itemMyCommentsName.text=name
        binding.itemMyCommentsUserGender.text=
            when (gender) {
            "m" -> "(绅士)"
            "f" -> "(淑女)"
            else -> "(机器人)"}
        binding.itemMyCommentsUserLevel.text="Lv.$level"
        binding.itemMyCommentsTime.text=TimeUtil().time(bean.created_at)

        if (fileServer != "") {//头像

            Glide.with(holder.itemView)
                .load(
                    GlideUrlNewKey(
                        fileServer,
                        path
                    )
                )
                .centerCrop()
                .placeholder(R.drawable.placeholder_avatar_2)
                .into(binding.itemMyCommentsUserImage)
        }
        if (character != "") { //头像框 新用户没有

            Glide.with(holder.itemView)
                .load(character)
                .into(binding.itemMyCommentsUserCharacter)
        }

        holder.addOnClickListener(R.id.item_my_comments_like_layout)
        holder.addOnClickListener(R.id.item_my_comments_sub_layout)
        holder.addOnClickListener(R.id.item_my_comments_title_layout)
    }

    override fun bindViewPayloads(
        holder: BaseBindingHolder<*, *>,
        bean: MyCommentsBean.Comments.Doc,
        binding: ItemMyCommentsBinding,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.bindViewPayloads(holder, bean, binding, position, payloads)

        for (p in payloads) {
            bean.isLiked =p as Boolean
            if (bean.isLiked) {
                bean.likesCount++
            } else {
                bean.likesCount--
            }
            binding.itemMyCommentsLikeText.text = bean.likesCount.toString()
            binding.itemMyCommentsLikeImage.setImageResource(if (bean.isLiked) R.drawable.ic_favorite_24 else R.drawable.ic_favorite_border_24)
        }
    }
}