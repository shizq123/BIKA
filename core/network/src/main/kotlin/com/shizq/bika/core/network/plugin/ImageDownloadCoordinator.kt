package com.shizq.bika.core.network.plugin

import coil3.annotation.ExperimentalCoilApi
import coil3.fetch.FetchResult
import coil3.network.ConcurrentRequestStrategy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes fetches for the same cache key. After a preload writes the image to disk,
 * the visible request rechecks that entry instead of downloading the same bytes again.
 * Mirror races must remain independent so a stalled primary cannot block its own fallback.
 */
@OptIn(ExperimentalCoilApi::class)
internal class ImageDownloadCoordinator : ConcurrentRequestStrategy {
    private val requests = KeyedRequestCoordinator()

    override suspend fun apply(key: String, block: suspend () -> FetchResult): FetchResult =
        if (currentCoroutineContext()[FallbackMarker] != null) block()
        else requests.withKey(key, block)
}

/** Entries include waiting callers, and are released even when cancelled before acquiring. */
internal class KeyedRequestCoordinator {
    private class Entry(val mutex: Mutex = Mutex(), var users: Int = 0)
    private val entries = mutableMapOf<String, Entry>()

    suspend fun <T> withKey(key: String, block: suspend () -> T): T {
        val entry = synchronized(entries) {
            entries.getOrPut(key) { Entry() }.also { it.users++ }
        }
        try {
            return entry.mutex.withLock { block() }
        } finally {
            synchronized(entries) {
                if (--entry.users == 0) entries.remove(key)
            }
        }
    }
}
