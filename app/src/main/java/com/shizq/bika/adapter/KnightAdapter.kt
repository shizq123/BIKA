package com.shizq.bika.adapter

import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.KnightBean
import com.shizq.bika.databinding.ItemKnightBinding
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey

class KnightAdapter : BaseBindingAdapter<KnightBean.Users, ItemKnightBinding>(R.layout.item_knight) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: KnightBean.Users,
        binding: ItemKnightBinding,
        position: Int
    ) {
        //头像
        GlideApp.with(holder.itemView)
            .load(
                if (bean.avatar != null)
                    GlideUrlNewKey(bean.avatar.fileServer, bean.avatar.path)
                else
                    R.drawable.placeholder_avatar_2
            )
            .placeholder(R.drawable.placeholder_avatar_2)
            .into(binding.knightUserImage)
        //头像框
        GlideApp.with(holder.itemView)
            .load(
                //https://pica-web.wakamoment.tk/ 网站失效 替换到能用的
                if (!bean.character.isNullOrEmpty())
                    bean.character.replace(
                        "pica-web.wakamoment.tk",
                        "pica-pica.wikawika.xyz"
                    )
                else ""
            )
            .into(binding.knightUserCharacter)

        holder.addOnClickListener(R.id.knight_user_image_layout)
    }
}