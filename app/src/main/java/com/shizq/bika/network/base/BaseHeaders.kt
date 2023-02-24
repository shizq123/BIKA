package com.shizq.bika.network.base

import com.shizq.bika.MyApp
import com.shizq.bika.network.HmacSHA256Util
import com.shizq.bika.utils.SPUtil
import java.util.*

class BaseHeaders(private val urlEnd: String = "", val type: String = "") {

    fun getHeaders(): MutableMap<String, String> {
        val apikey = "C69BAF41DA5ABD1FFEDC6D2FEA56B"
        val nonce = UUID.randomUUID().toString().replace("-", "")
        val time = (System.currentTimeMillis() / 1000).toString()
        val signature = HmacSHA256Util().C((urlEnd + time + nonce + type + apikey))
        val map: MutableMap<String, String> = HashMap()
        map["api-key"] = apikey
        map["accept"] = "application/vnd.picacomic.com.v1+json"
        map["app-channel"] = SPUtil.get(MyApp.contextBase, "setting_app_channel", "2") as String
        map["time"] = time
        map["nonce"] = nonce
        map["signature"] = signature
        map["app-version"] = "2.2.1.3.3.4"
        map["app-uuid"] = "defaultUuid"
        map["image-quality"] = "original"
        map["app-platform"] = "android"
        map["app-build-version"] = "45"
        map["user-agent"] = "okhttp/3.8.1"
        return map
    }

    fun getHeaderMapAndToken(): Map<String, String> {
        val map = getHeaders()
        map["authorization"] = SPUtil.get(MyApp.contextBase, "token", "") as String
        return map
    }

    fun getChatHeaders(): MutableMap<String, String> {
        val map: MutableMap<String, String> = HashMap()
        map["user-agent"] = "Dart/2.19 (dart:io)"
        map["api-version"] = "1.0.2"
        map["content-type"] = "application/json; charset=UTF-8"
        return map
    }

    fun getChatHeaderMapAndToken(): MutableMap<String, String> {
        val map = getChatHeaders()
        map["authorization"] = "Bearer " + SPUtil.get(MyApp.contextBase, "chat_token", "") as String
        return map
    }
}