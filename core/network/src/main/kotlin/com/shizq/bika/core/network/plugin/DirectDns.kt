package com.shizq.bika.core.network.plugin

import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.network.BikaEndpoints
import io.github.oshai.kotlinlogging.KotlinLogging
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

private val logger = KotlinLogging.logger("DirectDns")

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
                            logger.warn(e) { "Invalid API IP string: $ip" }
                            null
                        }
                    }
                    val imageIps = userData.network.dns.imageDnsHosts.mapNotNull { ip ->
                        try {
                            InetAddress.getByName(ip)
                        } catch (e: UnknownHostException) {
                            logger.warn(e) { "Invalid Image IP string: $ip" }
                            null
                        }
                    }

                    apiIpsRef.store(apiIps)
                    imageIpsRef.store(imageIps)
                    logger.info { "Direct DNS updated. API IPs: ${apiIps.size}, Image IPs: ${imageIps.size}" }
                }
        }
    }

    override fun lookup(hostname: String): List<InetAddress> {
        if (isApiHost(hostname)) {
            val currentApiIps = apiIpsRef.load()
            if (currentApiIps.isNotEmpty()) {
                logger.debug { "Returning API IP list for hostname: $hostname" }
                return currentApiIps
            }
        } else if (isImageHost(hostname)) {
            val currentImageIps = imageIpsRef.load()
            if (currentImageIps.isNotEmpty()) {
                logger.debug { "Returning Image IP list for hostname: $hostname" }
                return currentImageIps
            }
        }

        logger.debug { "No direct IP matched or empty IP list. Falling back to system DNS for: $hostname" }
        return Dns.SYSTEM.lookup(hostname)
    }

    private fun isApiHost(hostname: String): Boolean = hostname.matchesHost(BikaEndpoints.API_HOST)

    private fun isImageHost(hostname: String): Boolean {
        if (isApiHost(hostname)) return false
        return IMAGE_HOST_SUFFIXES.any { hostname.matchesHost(it) }
    }

    private fun String.matchesHost(domain: String): Boolean =
        equals(domain, ignoreCase = true) || endsWith(".$domain", ignoreCase = true)

    private companion object {
        val IMAGE_HOST_SUFFIXES = listOf("picacomic.com", "diwodiwo.xyz", "tipatipa.xyz")
    }
}