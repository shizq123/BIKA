package com.shizq.bika.core.network.plugin

import android.util.Log
import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.network.BuildConfig
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Singleton
class DirectDns @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    @ApplicationScope private val scope: CoroutineScope,
) : Dns {
    private val apiIpsRef = AtomicReference<List<InetAddress>>(emptyList())
    private val imageIpsRef = AtomicReference<List<InetAddress>>(emptyList())

    init {
        scope.launch(Dispatchers.IO) {
            userPreferencesDataSource.userData
                .distinctUntilChanged { old, new ->
                    old.network.dns.apiDnsHosts == new.network.dns.apiDnsHosts && old.network.dns.imageDnsHosts == new.network.dns.imageDnsHosts
                }
                .collect { userData ->
                    val apiIps = userData.network.dns.apiDnsHosts.mapNotNull { ip ->
                        try {
                            InetAddress.getByName(ip)
                        } catch (e: UnknownHostException) {
                            if (DEBUG_LOGGING) Log.w(TAG, "Invalid API IP string: $ip", e)
                            null
                        }
                    }
                    val imageIps = userData.network.dns.imageDnsHosts.mapNotNull { ip ->
                        try {
                            InetAddress.getByName(ip)
                        } catch (e: UnknownHostException) {
                            if (DEBUG_LOGGING) Log.w(TAG, "Invalid Image IP string: $ip", e)
                            null
                        }
                    }

                    apiIpsRef.store(apiIps)
                    imageIpsRef.store(imageIps)
                    if (DEBUG_LOGGING) Log.i(TAG, "Direct DNS updated. API IPs: ${apiIps.size}, Image IPs: ${imageIps.size}")
                }
        }
    }

    override fun lookup(hostname: String): List<InetAddress> {
        if (isApiHost(hostname)) {
            val currentApiIps = apiIpsRef.load()
            if (currentApiIps.isNotEmpty()) {
                if (DEBUG_LOGGING) Log.d(TAG, "Returning API IP list for hostname: $hostname")
                return currentApiIps
            }
        } else if (isImageHost(hostname)) {
            val currentImageIps = imageIpsRef.load()
            if (currentImageIps.isNotEmpty()) {
                if (DEBUG_LOGGING) Log.d(TAG, "Returning Image IP list for hostname: $hostname")
                return currentImageIps
            }
        }

        if (DEBUG_LOGGING) Log.d(TAG, "No direct IP matched or empty IP list. Falling back to system DNS for: $hostname")
        return Dns.SYSTEM.lookup(hostname)
    }

    private fun isApiHost(hostname: String): Boolean {
        return hostname.contains("picaapi.picacomic.com", ignoreCase = true)
    }

    private fun isImageHost(hostname: String): Boolean {
        if (hostname.contains("picaapi.picacomic.com", ignoreCase = true)) return false
        return hostname.contains("picacomic.com", ignoreCase = true) ||
               hostname.contains("diwodiwo.xyz", ignoreCase = true) ||
               hostname.contains("tipatipa.xyz", ignoreCase = true)
    }
}

private const val TAG = "DirectDns"
private val DEBUG_LOGGING = BuildConfig.DEBUG