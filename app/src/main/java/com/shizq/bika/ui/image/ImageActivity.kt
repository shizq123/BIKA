package com.shizq.bika.ui.image

import android.os.Bundle
import androidx.core.view.ViewCompat
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityImageBinding
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey

//图片展示
class ImageActivity : BaseActivity<ActivityImageBinding, ImageViewModel>() {

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_image
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    override fun initData() {
        ViewCompat.setTransitionName(binding.touchImageView,"image")
        val fileserver = intent.getStringExtra("fileserver") as String
        val imageurl = intent.getStringExtra("imageurl") as String

        GlideApp
            .with(this)
            .load(
                if (imageurl != "") {
                    GlideUrlNewKey(fileserver, imageurl)
                } else {
                    R.drawable.placeholder_transparent_low
                }
            )
//            .placeholder(R.drawable.placeholder_transparent_low)
            .into(binding.touchImageView)

        binding.touchImageView.setOnClickListener {
            finishAfterTransition()
        }
    }
}