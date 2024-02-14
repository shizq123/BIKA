package com.shizq.bika.adapter

import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.EpisodeBean
import com.shizq.bika.databinding.ItemEpisodeBinding
import com.shizq.bika.utils.TimeUtil

//漫画片段或章节adapter
class EpisodeAdapter : BaseBindingAdapter<EpisodeBean.Eps.Doc, ItemEpisodeBinding>(R.layout.item_episode) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: EpisodeBean.Eps.Doc,
        binding: ItemEpisodeBinding,
        position: Int
    ) {
        binding.episodeTitle.text = bean.title
        binding.episodeTime.text = TimeUtil().time(bean.updated_at)
    }
}