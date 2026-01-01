package com.shizq.bika.core.network.plugin

import android.util.Log
import kotlinx.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

object ConnectionWarmer {
    fun warmUp(client: OkHttpClient, urls: List<String>) {
        val uniqueHosts = urls.toSet()

        uniqueHosts.forEach { host ->
            val url = "https://${host}/"
            val request = Request.Builder()
                .url(url)
                .head()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.w("ConnectionWarmer", "Failed to warm up connection to $host", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.i(
                        "ConnectionWarmer",
                        "Connection to $host warmed up. Response: ${response.code}"
                    )
                    response.close()
                }
            })
        }
    }
}