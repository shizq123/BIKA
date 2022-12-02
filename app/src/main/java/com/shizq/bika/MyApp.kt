package com.shizq.bika

import android.annotation.SuppressLint
import android.app.Application
import com.google.android.material.color.DynamicColors

class MyApp : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var contextBase: Application
    }

    override fun onCreate() {
        super.onCreate()
        contextBase = this
        DynamicColors.applyToActivitiesIfAvailable(this)//根据壁纸修改App主题颜色

    }
}