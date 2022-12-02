package com.shizq.bika.adapter

import android.view.View
import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ComicListBean2
import com.shizq.bika.databinding.ItemComiclist2Binding
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey

//漫画列表的第二种数据类型
class ComicListAdapter2 : BaseBindingAdapter<ComicListBean2.Comics, ItemComiclist2Binding>(R.layout.item_comiclist2) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: ComicListBean2.Comics,
        binding: ItemComiclist2Binding,
        position: Int
    ) {
        if (bean.likesCount != 0) {
            binding.comiclistItemLikeimage.visibility = View.VISIBLE
            binding.comiclistItemLiketext.visibility = View.VISIBLE
            binding.comiclistItemLiketext.text = bean.likesCount.toString()
        } else {
            binding.comiclistItemLikeimage.visibility = View.GONE
            binding.comiclistItemLiketext.visibility = View.GONE
        }
        if (bean.epsCount != 0) {
            if (bean.finished) {
                binding.comiclistItemBooktext.text =
                    "${bean.epsCount}C/${bean.pagesCount}P(完)"
            } else {
                binding.comiclistItemBooktext.text =
                    "${bean.epsCount}C/${bean.pagesCount}P"
            }
        } else {
            binding.comiclistItemBooktext.text = ""
        }

        if (bean.leaderboardCount != 0) {
            binding.comiclistItemClicks.visibility = View.VISIBLE
            binding.comiclistItemClicks.text = "指数：${bean.leaderboardCount}"
        } else {
            if (bean.totalViews != 0) {
                binding.comiclistItemClicks.visibility = View.VISIBLE
                binding.comiclistItemClicks.text = "指数：${bean.totalViews}"
            } else {
                binding.comiclistItemClicks.visibility = View.GONE
            }
        }

        var ategory = ""
        for (i in bean.categories) {
            ategory = "$ategory $i"
        }
        binding.comiclistItemCategory.text = "分类：$ategory"

        GlideApp.with(holder.itemView)
            .load(
                //哔咔服务器问题 需自行修改图片请求路径
                GlideUrlNewKey(
                    bean.thumb.fileServer, bean.thumb.path
                )
            )
            .centerCrop()
            .placeholder(R.drawable.placeholder_transparent)
            .into(binding.comiclistItemImage)
        holder.addOnClickListener(R.id.comiclist_item_image)

    }
}