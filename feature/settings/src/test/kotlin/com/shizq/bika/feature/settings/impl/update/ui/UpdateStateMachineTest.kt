package com.shizq.bika.feature.settings.impl.update.ui

import androidx.datastore.core.DataStore
import com.shizq.bika.core.data.platform.AppVersionProvider
import com.shizq.bika.core.data.platform.UpdateFileProvider
import com.shizq.bika.core.data.repository.AppRelease
import com.shizq.bika.core.data.repository.AppUpdateRepository
import com.shizq.bika.core.datastore.UpdatePreferenceDataSource
import com.shizq.bika.core.datastore.model.UpdatePreference
import com.shizq.bika.core.domain.CheckAppUpdateUseCase
import com.shizq.bika.core.domain.DownloadUpdateApkUseCase
import com.shizq.bika.core.domain.IgnoreUpdateVersionUseCase
import com.shizq.bika.core.domain.MarkUpdatePromptedUseCase
import com.shizq.bika.core.domain.ShouldShowUpdateUseCase
import com.shizq.bika.core.model.AppUpdateRelease
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateStateMachineTest {

    private class InMemoryDataStore(
        initial: UpdatePreference = UpdatePreference(),
    ) : DataStore<UpdatePreference> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<UpdatePreference> = state
        override suspend fun updateData(transform: suspend (UpdatePreference) -> UpdatePreference): UpdatePreference {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private class FakeAppUpdateRepository(
        var release: AppRelease? = null,
    ) : AppUpdateRepository {
        override suspend fun checkForUpdate(currentVersionName: String): AppRelease? = release

        override suspend fun downloadApk(
            downloadUrl: String,
            destFile: File,
            onProgress: (Float) -> Unit,
        ) {
            onProgress(0.5f)
            destFile.parentFile?.mkdirs()
            destFile.writeText("fake apk content")
            onProgress(1.0f)
        }
    }

    private class FakeAppVersionProvider(
        override val versionName: String = "1.0.0",
        override val versionCode: Long = 100L,
    ) : AppVersionProvider

    private class FakeUpdateFileProvider(
        private val tempDir: File,
    ) : UpdateFileProvider {
        override fun getApkFile(versionCode: Long): File {
            return File(tempDir, "test_update_$versionCode.apk")
        }
    }

    private fun createTestFixture(
        remoteRelease: AppRelease? = null,
        initialPreference: UpdatePreference = UpdatePreference(),
        tempDir: File = File(System.getProperty("java.io.tmpdir"), "bika_test_${System.currentTimeMillis()}"),
    ): TestFixture {
        val dataStore = InMemoryDataStore(initialPreference)
        val prefDataSource = UpdatePreferenceDataSource(dataStore)
        val updateRepo = FakeAppUpdateRepository(remoteRelease)
        val versionProvider = FakeAppVersionProvider()
        val fileProvider = FakeUpdateFileProvider(tempDir)

        val checkUseCase = CheckAppUpdateUseCase(updateRepo, versionProvider)
        val shouldShowUseCase = ShouldShowUpdateUseCase(prefDataSource)
        val markPromptedUseCase = MarkUpdatePromptedUseCase(prefDataSource)
        val ignoreUseCase = IgnoreUpdateVersionUseCase(prefDataSource)
        val downloadUseCase = DownloadUpdateApkUseCase(updateRepo, fileProvider)
        val effectEmitter = UpdateEffectEmitter()

        val stateMachine = UpdateStateMachine(
            checkAppUpdateUseCase = checkUseCase,
            shouldShowUpdateUseCase = shouldShowUseCase,
            markUpdatePromptedUseCase = markPromptedUseCase,
            ignoreUpdateVersionUseCase = ignoreUseCase,
            downloadUpdateApkUseCase = downloadUseCase,
            effectEmitter = effectEmitter,
        )

        return TestFixture(
            stateMachine = stateMachine,
            effectEmitter = effectEmitter,
            prefDataSource = prefDataSource,
            tempDir = tempDir,
        )
    }

    private data class TestFixture(
        val stateMachine: UpdateStateMachine,
        val effectEmitter: UpdateEffectEmitter,
        val prefDataSource: UpdatePreferenceDataSource,
        val tempDir: File,
    )

    @Test
    fun `启动自动检查更新发现新版本时进入 HasUpdate 且不提前记录免打扰时间`() = runTest {
        val remote = AppRelease(
            remoteVersion = "1.1.0",
            changelog = "新增功能",
            downloadUrl = "https://example.com/test.apk",
        )
        val fixture = createTestFixture(remoteRelease = remote)
        val launched = fixture.stateMachine.launchIn(backgroundScope)

        launched.dispatch(UpdateAction.CheckUpdate(source = UpdateCheckSource.Auto))

        val state = launched.state.first { it is UpdateUiState.HasUpdate } as UpdateUiState.HasUpdate
        assertEquals("1.1.0", state.release.versionName)

        // 核心验证：检查成功时不能提前写入 lastPromptTime
        assertNull(fixture.prefDataSource.getLastPromptTimeMillis())
    }

    @Test
    fun `用户点击稍后时记录免打扰时间戳并回到 Idle`() = runTest {
        val remote = AppRelease(
            remoteVersion = "1.1.0",
            changelog = "修复 bug",
            downloadUrl = "https://example.com/test.apk",
        )
        val fixture = createTestFixture(remoteRelease = remote)
        val launched = fixture.stateMachine.launchIn(backgroundScope)

        // 触发检查并等待进入 HasUpdate 状态
        launched.dispatch(UpdateAction.CheckUpdate(source = UpdateCheckSource.Auto))
        val hasUpdate = launched.state.first { it is UpdateUiState.HasUpdate } as UpdateUiState.HasUpdate

        // 发送稍后操作
        launched.dispatch(UpdateAction.RemindLater(hasUpdate.release))

        launched.state.first { it is UpdateUiState.Idle }
        assertNotNull(fixture.prefDataSource.getLastPromptTimeMillis())
    }

    @Test
    fun `用户确认后下载完成会发送 InstallApk 事件并回到 Idle`() = runTest {
        val remote = AppRelease(
            remoteVersion = "1.1.0",
            changelog = "版本更新",
            downloadUrl = "https://example.com/test.apk",
        )
        val fixture = createTestFixture(remoteRelease = remote)
        val launched = fixture.stateMachine.launchIn(backgroundScope)

        launched.dispatch(UpdateAction.CheckUpdate(source = UpdateCheckSource.Auto))
        val hasUpdate = launched.state.first { it is UpdateUiState.HasUpdate } as UpdateUiState.HasUpdate

        launched.dispatch(UpdateAction.StartDownload(hasUpdate.release))

        val effect = withTimeoutOrNull(2_000) {
            fixture.effectEmitter.effects.first()
        }

        assertTrue(effect is UpdateUiEffect.InstallApk)
        val installEffect = effect as UpdateUiEffect.InstallApk
        assertTrue(File(installEffect.apkPath).exists())

        launched.state.first { it is UpdateUiState.Idle }

        // 清理临时文件
        fixture.tempDir.deleteRecursively()
    }

    @Test
    fun `用户点击忽略此版本时写入偏好设置并回到 Idle`() = runTest {
        val remote = AppRelease(
            remoteVersion = "1.1.0",
            changelog = "修复已知问题",
            downloadUrl = "https://example.com/test.apk",
        )
        val fixture = createTestFixture(remoteRelease = remote)
        val launched = fixture.stateMachine.launchIn(backgroundScope)

        launched.dispatch(UpdateAction.CheckUpdate(source = UpdateCheckSource.Auto))
        val hasUpdate = launched.state.first { it is UpdateUiState.HasUpdate } as UpdateUiState.HasUpdate

        launched.dispatch(UpdateAction.IgnoreVersion(hasUpdate.release))

        launched.state.first { it is UpdateUiState.Idle }
        assertEquals(hasUpdate.release.versionCode, fixture.prefDataSource.getIgnoredVersionCode())
    }
}
