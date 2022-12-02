package com.shizq.bika.network

import com.shizq.bika.MyApp
import com.shizq.bika.utils.SPUtil
import okhttp3.Dns
import java.net.InetAddress

class HttpDns:Dns {
    private val DNS_SYSTEM=Dns.SYSTEM

    override fun lookup(hostname: String): MutableList<InetAddress> {

        if (hostname != "68.183.234.72" ){
            //dns host 不是这个ip时用其他ip
            val host=SPUtil.get(MyApp.contextBase,"addresses",hostname) as String

            return DNS_SYSTEM.lookup(host)
        }
        return DNS_SYSTEM.lookup(hostname)
    }
}