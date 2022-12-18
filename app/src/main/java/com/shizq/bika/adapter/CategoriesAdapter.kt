package com.shizq.bika.adapter
import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.CategoriesBean
import com.shizq.bika.databinding.ItemCategoriesBinding
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey

//分类
class CategoriesAdapter :
    BaseBindingAdapter<CategoriesBean.Category, ItemCategoriesBinding>(R.layout.item_categories) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: CategoriesBean.Category,
        binding: ItemCategoriesBinding,
        position: Int
    ) {
        GlideApp.with(holder.itemView)
            .load(
                //判断是否是手动添加的数据
                if (bean.imageRes == null) {
                    //哔咔服务器问题 需自行修改图片请求路径
                    GlideUrlNewKey("https://s3.picacomic.com", bean.thumb.path)
                } else {
                    bean.imageRes
                }
            )
            .centerCrop()
            .placeholder(R.drawable.placeholder_transparent)
            .into(binding.categoriesItemImage)
    }

}