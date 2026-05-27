package com.shizq.bika.ui.image

import androidx.appcompat.app.AppCompatActivity

/**
 * 图片展示与大图查看页面 (ImageActivity)
 *
 * 【功能说明】
 * 1. 用于全屏查看单张图片（大图预览）。
 * 2. 接收 Intent 传递过来的图片服务器地址 (fileserver) 和图片相对路径 (imageurl)。
 * 3. 使用 Glide 加载图片到 TouchImageView 控件中，支持手势缩放等交互。
 * 4. 支持共享元素转场动画 (Transition)，点击图片时会带转场动画退出页面 (finishAfterTransition)。
 *
 * 注意：目前该 Activity 的核心初始化和加载逻辑被注释，若需启用需解开相应注释并确保
 * 相关的布局 (activity_image)、绑定对象 (binding)、以及辅助类 (GlideUrlNewKey) 存在并配置正确。
 */
class ImageActivity : AppCompatActivity() {
//
//    override fun initContentView(savedInstanceState: Bundle?): Int {
//        return R.layout.activity_image
//    }
//
//    override fun initVariableId(): Int {
//        return BR.viewModel
//    }
//
//    override fun initData() {
//        ViewCompat.setTransitionName(binding.touchImageView,"image")
//        val fileserver = intent.getStringExtra("fileserver") as String
//        val imageurl = intent.getStringExtra("imageurl") as String
//
//        Glide
//            .with(this)
//            .load(
//                if (imageurl != "") {
//                    GlideUrlNewKey(fileserver, imageurl)
//                } else {
//                    R.drawable.placeholder_transparent_low
//                }
//            )
////            .placeholder(R.drawable.placeholder_transparent_low)
//            .into(binding.touchImageView)
//
//        binding.touchImageView.setOnClickListener {
//            finishAfterTransition()
//        }
//    }
//}
}