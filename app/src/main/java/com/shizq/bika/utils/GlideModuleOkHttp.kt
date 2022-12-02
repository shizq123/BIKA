package com.shizq.bika.utils

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.shizq.bika.network.HttpDns
import okhttp3.OkHttpClient
import java.io.InputStream

/**
 * 修改Glide的DNS
 */

@GlideModule
class GlideModuleOkHttp : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // 自定义OkHttpClient
        val okHttpClient = OkHttpClient.Builder()
            .dns(HttpDns())
            .build()
        // 采用自定义的CustomOkHttpUrlLoader
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java, OkHttpUrlLoader.Factory(okHttpClient)
        )
    }
}