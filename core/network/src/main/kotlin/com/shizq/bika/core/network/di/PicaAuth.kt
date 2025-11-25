package com.shizq.bika.core.network.di

import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal object PicaAuth {
    const val DIGEST_KEY = $$"~d}$Q7$eIni=V)9\\RK/P.RM4;9[7|@/CA}b~OW!3?EV`:<>M7pddUBL5n|0/*Cn"
    const val API_KEY = "C69BAF41DA5ABD1FFEDC6D2FEA56B"

    private fun getAppChannel(): String = "1"

    @OptIn(ExperimentalUuidApi::class)
    fun generate(path: String, method: String): Map<String, String> {
        val cleanPath = path.removePrefix("/")

        // 2. 准备动态参数
        val nonce = Uuid.random().toString().replace("-", "")
        val time = (System.currentTimeMillis() / 1000).toString()


        // urlEnd + time + nonce + type + apikey
        val rawData = cleanPath + time + nonce + method + API_KEY
        val signature = hmacSha256(rawData)

        // 4. 组装 Header
        return mapOf(
            "api-key" to API_KEY,
            "accept" to "application/vnd.picacomic.com.v1+json",
            "app-channel" to getAppChannel(),
            "time" to time,
            "nonce" to nonce,
            "signature" to signature,
            "app-version" to "2.2.1.3.3.4",
            "app-uuid" to "defaultUuid",
            "image-quality" to "original",
            "app-platform" to "android",
            "app-build-version" to "45",
            "user-agent" to "okhttp/3.8.1"
        )
    }

    private fun hmacSha256(data: String): String {
        return HmacUtils(HmacAlgorithms.HMAC_SHA_256, DIGEST_KEY).hmacHex(data)
    }
}