package com.shizq.bika.adapter

import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.PicaAppsBean
import com.shizq.bika.databinding.ItemPicaappsBinding
import com.bumptech.glide.Glide

class PicaAppsAdapter :
    BaseBindingAdapter<PicaAppsBean.App, ItemPicaappsBinding>(R.layout.item_picaapps) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: PicaAppsBean.App,
        binding: ItemPicaappsBinding,
        position: Int
    ) {

        Glide.with(holder.itemView)
            .load(bean.icon)
            .placeholder(R.drawable.placeholder_avatar_2)
            .into(binding.picaAppsImage)

    }
}