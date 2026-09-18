package com.shizq.bika.core.network.plugin

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.selects.select

/** A slow primary can still win after fallback starts; failures alone never win the race. */
internal suspend fun <T : Any> awaitPrimaryOrFallback(
    primary: Deferred<T>,
    fallback: Deferred<T?>,
    isSuccess: (T) -> Boolean,
): T = try {
    select {
        primary.onAwait { result ->
            if (isSuccess(result)) result else fallback.await() ?: result
        }
        fallback.onAwait { result -> result ?: primary.await() }
    }
} finally {
    primary.cancel()
    fallback.cancel()
}
