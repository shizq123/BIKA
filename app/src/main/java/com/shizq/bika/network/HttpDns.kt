package com.shizq.bika.network

import com.shizq.bika.BIKAApplication
import com.shizq.bika.utils.SPUtil
import okhttp3.Dns
import java.net.InetAddress

class HttpDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        //这个sp影响加载速度 以后再说 //直接默认走第一个ip不改了
        val host = SPUtil.get(BIKAApplication.contextBase, "addresses1", hostname) as String //获得分流对应的hostname
        return Dns.SYSTEM.lookup(host)
    }
}