package com.shizq.bika.feature.settings.impl.update.ui

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateEffectEmitterTest {
    @Test
    fun `download completed without a collector is delivered when the UI returns`() = runTest {
        val emitter = UpdateEffectEmitter()
        val effect = UpdateUiEffect.InstallApk("update.apk")

        emitter.emit(effect)

        assertEquals(effect, withTimeoutOrNull(1_000) { emitter.effects.first() })
    }

    @Test
    fun `consumed installation is not repeated when the collector restarts`() = runTest {
        val emitter = UpdateEffectEmitter()
        val effect = UpdateUiEffect.InstallApk("update.apk")
        val received = async { emitter.effects.first() }
        emitter.emit(effect)
        assertEquals(effect, received.await())

        assertNull(withTimeoutOrNull(1_000) { emitter.effects.first() })
    }

    @Test
    fun `separate update flows do not receive each others installation`() = runTest {
        val first = UpdateEffectEmitter()
        val second = UpdateEffectEmitter()
        val effect = UpdateUiEffect.InstallApk("first.apk")

        first.emit(effect)

        assertNull(withTimeoutOrNull(1_000) { second.effects.first() })
        assertEquals(effect, withTimeoutOrNull(1_000) { first.effects.first() })
    }
}
