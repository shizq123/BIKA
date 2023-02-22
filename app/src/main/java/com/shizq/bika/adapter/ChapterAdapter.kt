package com.shizq.bika.adapter

import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ChapterBean
import com.shizq.bika.databinding.ItemChapterBinding
import com.shizq.bika.utils.TimeUtil

class ChapterAdapter : BaseBindingAdapter<ChapterBean.Eps.Doc, ItemChapterBinding>(R.layout.item_chapter) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: ChapterBean.Eps.Doc,
        binding: ItemChapterBinding,
        position: Int
    ) {
        binding.chapterTitle.text = bean.title
        binding.chapterTime.text = TimeUtil().time(bean.updated_at)
    }
}