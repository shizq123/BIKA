package com.shizq.bika

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy.Builder
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.shizq.bika.sync.initializers.Sync
import com.shizq.bika.util.ProfileVerifierLogger
import com.shizq.bika.utils.SPUtil
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject

@HiltAndroidApp
class BikaApplication : Application(), SingletonImageLoader.Factory {
    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var profileVerifierLogger: ProfileVerifierLogger
    override fun onCreate() {
        super.onCreate()

        setStrictModePolicy()

        SPUtil.init(this)
        Sync.initialize(this)
        profileVerifierLogger()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader

    /**
     * Return true if the application is debuggable.
     */
    private fun isDebuggable(): Boolean {
        return 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
    }

    /**
     * Set a thread policy that detects all potential problems on the main thread, such as network
     * and disk access.
     *
     * If a problem is found, the offending call will be logged and the application will be killed.
     */
    private fun setStrictModePolicy() {
        if (isDebuggable()) {
            StrictMode.setThreadPolicy(
                Builder().detectAll().penaltyLog().build(),
            )
        }
    }
}
