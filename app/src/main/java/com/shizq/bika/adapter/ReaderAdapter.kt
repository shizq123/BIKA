package com.shizq.bika.adapter

import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ComicsPictureBean
import com.shizq.bika.databinding.ItemPictureBinding
import com.bumptech.glide.Glide
import com.shizq.bika.utils.GlideUrlNewKey

class ReaderAdapter : BaseBindingAdapter<ComicsPictureBean.Pages.Docs, ItemPictureBinding>(R.layout.item_picture) {

    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: ComicsPictureBean.Pages.Docs,
        binding: ItemPictureBinding,
        position: Int
    ) {
        binding.itemPictureText.visibility = View.VISIBLE
        binding.itemPictureText.text = "${position + 1}"

        Glide.with(holder.itemView)
            .asDrawable()
            .placeholder(R.drawable.placeholder_transparent_low)//占位图.
            .format(DecodeFormat.PREFER_RGB_565)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)//只缓存处理后的图片到磁盘
            .load(GlideUrlNewKey(bean.media.fileServer, bean.media.path))
            .dontAnimate()
            .dontTransform()
            .into(object : CustomTarget<Drawable>(1080, 1) {
                override fun onLoadStarted(placeholder: Drawable?) {
                    super.onLoadStarted(placeholder)
                    binding.itemPictureText.visibility = View.VISIBLE
                    binding.itemPictureImage.setImageDrawable(placeholder)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    binding.itemPictureText.visibility = View.GONE
                    binding.itemPictureImage.setImageDrawable(null)
                }

                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
//                    val h = resource.intrinsicHeight
//                    val w = resource.intrinsicWidth

                    binding.itemPictureText.visibility = View.GONE
                    binding.itemPictureImage.setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    binding.itemPictureText.visibility = View.VISIBLE
                    binding.itemPictureImage.setImageDrawable(placeholder)
                }

            })
    }
}