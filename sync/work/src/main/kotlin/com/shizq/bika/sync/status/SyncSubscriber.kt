package com.shizq.bika.sync.status

/**
 * Subscribes to backend requested synchronization
 */
interface SyncSubscriber {
    suspend fun subscribe()
}