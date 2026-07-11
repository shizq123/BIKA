package com.shizq.bika.core.network.di

import com.shizq.bika.core.network.dns.DnsHostResolver
import com.shizq.bika.core.network.dns.HostLatencyProbe
import com.shizq.bika.core.network.dns.KtorDnsHostResolver
import com.shizq.bika.core.network.dns.SocketHostLatencyProbe
import com.shizq.bika.core.network.image.CoilImageCacheManager
import com.shizq.bika.core.network.image.ImageCacheManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkToolsModule {

    @Binds
    abstract fun bindDnsHostResolver(impl: KtorDnsHostResolver): DnsHostResolver

    @Binds
    abstract fun bindHostLatencyProbe(impl: SocketHostLatencyProbe): HostLatencyProbe

    @Binds
    abstract fun bindImageCacheManager(impl: CoilImageCacheManager): ImageCacheManager
}
