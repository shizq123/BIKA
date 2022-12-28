package com.shizq.bika.bean

data class PicaAppsBean(
    val apps: List<App>
){
    data class App(
        val description: String,
        val icon: String,
        val showTitleBar: Boolean,
        val title: String,
        val url: String
    )
}

