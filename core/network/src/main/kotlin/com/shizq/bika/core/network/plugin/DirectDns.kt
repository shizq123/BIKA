package com.shizq.bika.core.network.plugin

import android.util.Log
import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private val ipListRef = AtomicReference<List<InetAddress>>(emptyList())

    init {
        scope.launch(Dispatchers.IO) {
            userPreferencesDataSource.userData
                .map { it.dns }
                .distinctUntilChanged()
                .collect { dnsStrings ->
                    val newIpList = dnsStrings.mapNotNull { ip ->
                        try {
                            InetAddress.getByName(ip)
                        } catch (e: UnknownHostException) {
                            Log.w(TAG, "Invalid IP string received: $ip", e)
                            null
                        }
                    }

                    ipListRef.store(newIpList)
                    Log.i(TAG, "Global IP list updated with ${newIpList.size} addresses.")
                }
        }
    }

    override fun lookup(hostname: String): List<InetAddress> {
        val currentIps = ipListRef.load()

        if (currentIps.isNotEmpty()) {
            Log.d(TAG, "Returning global IP list for hostname: $hostname")
            return currentIps
        }

        Log.d(TAG, "Global IP list is empty. Falling back to system DNS for: $hostname")
        return Dns.SYSTEM.lookup(hostname)
    }
}

private const val TAG = "DirectDns"