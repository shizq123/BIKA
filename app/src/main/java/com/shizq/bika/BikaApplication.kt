package com.shizq.bika

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.shizq.bika.sync.initializers.Sync
import com.shizq.bika.utils.SPUtil
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject

@HiltAndroidApp
class BikaApplication : Application(), SingletonImageLoader.Factory {
    @Inject
    lateinit var imageLoader: ImageLoader
    override fun onCreate() {
        super.onCreate()

        SPUtil.init(this)
        Sync.initialize(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
