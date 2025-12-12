package com.shizq.bika.ui

import kotlinx.coroutines.flow.Flow

interface Store<STATE, EVENT> {
    // Input
    suspend fun dispatch(action: EVENT)

    // Output
    val state: Flow<STATE>
}