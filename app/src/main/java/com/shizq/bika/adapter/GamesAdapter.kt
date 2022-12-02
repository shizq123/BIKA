package com.shizq.bika.adapter

import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.GamesBean
import com.shizq.bika.databinding.ItemGamesBinding
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey

class GamesAdapter :
    BaseBindingAdapter<GamesBean.Games.Docs, ItemGamesBinding>(R.layout.item_games) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: GamesBean.Games.Docs,
        binding: ItemGamesBinding,
        position: Int
    ) {
        GlideApp.with(holder.itemView)
            .load(GlideUrlNewKey(bean.icon.fileServer, bean.icon.path))
            .placeholder(R.drawable.placeholder_avatar_2)
            .into(binding.gamesItemImage)
    }
}