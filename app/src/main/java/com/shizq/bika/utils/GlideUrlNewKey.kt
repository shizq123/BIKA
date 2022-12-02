package com.shizq.bika.utils

import com.bumptech.glide.load.model.GlideUrl

//因为图片的ip地址经常改变，会占用手机存储空间和浪费手机流量，所以改变Glide缓存的key,来减少占用，Glide默认缓存的key是用图片的url，现在改成url后面的参数作为缓存key
class GlideUrlNewKey(baseUrl: String, private val key: String) : GlideUrl("$baseUrl/static/$key") {

    //再通过重写getCacheKey() 来改变本地缓存的key
    override fun getCacheKey(): String {
        //返回进行缓存的key
        return key
    }
}