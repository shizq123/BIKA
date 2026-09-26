package com.shizq.bika.util

import android.util.Log
import androidx.profileinstaller.ProfileVerifier
import com.shizq.bika.core.coroutine.ApplicationScope
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ProfileVerifierLogger @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) {
    companion object {
        private const val TAG = "ProfileInstaller"
    }

    operator fun invoke() = scope.launch {
        val deferredStatus = async {
            ProfileVerifier.getCompilationStatusAsync()
        }

        val future = deferredStatus.await()
        val status = future.get()

        logProfileStatus(status)
    }

    private fun logProfileStatus(status: ProfileVerifier.CompilationStatus) {
        Log.d(TAG, "Status code: ${status.profileInstallResultCode}")
        Log.d(
            TAG,
            when {
                status.isCompiledWithProfile -> "App compiled with profile"
                status.hasProfileEnqueuedForCompilation() -> "Profile enqueued for compilation"
                else -> "Profile not compiled nor enqueued"
            },
        )
    }
}