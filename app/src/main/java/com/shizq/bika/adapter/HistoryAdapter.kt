package com.shizq.bika.adapter

import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.databinding.ItemHistroyBinding
import com.shizq.bika.database.model.HistoryEntity
import com.bumptech.glide.Glide
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.TimeUtil

class HistoryAdapter :
    BaseBindingAdapter<HistoryEntity, ItemHistroyBinding>(R.layout.item_histroy) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: HistoryEntity,
        binding: ItemHistroyBinding,
        position: Int
    ) {
        Glide.with(holder.itemView)
            .load(GlideUrlNewKey(bean.fileServer, bean.path))
            .placeholder(R.drawable.placeholder_avatar_2)
            .into(binding.historyItemImage)
        binding.historyItemTime.text = TimeUtil().getDate(bean.time)
    }
}