package com.shizq.bika.adapter

import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.databinding.ItemHistroyBinding
import com.shizq.bika.db.History
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.TimeUtil

class HistoryAdapter :
    BaseBindingAdapter<History, ItemHistroyBinding>(R.layout.item_histroy) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: History,
        binding: ItemHistroyBinding,
        position: Int
    ) {
        GlideApp.with(holder.itemView)
            .load(GlideUrlNewKey(bean.fileServer, bean.path))
            .placeholder(R.drawable.placeholder_avatar_2)
            .into(binding.historyItemImage)
        binding.historyItemTime.text = TimeUtil().getDate(bean.time)
    }
}