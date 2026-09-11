package com.shizq.bika.core.datastore.serializer

import androidx.datastore.core.DataStore
import com.shizq.bika.core.model.preferences.UserPreferences
import com.shizq.bika.core.model.preferences.UserProfileSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 迁移是这次拆分里唯一会读写用户既有数据的部分，出错的表现是「升级后资料卡空白」
 * 或「旧字段永远清不掉」，都不会抛异常，所以用测试钉住行为。
 */
class UserProfileCacheMigrationTest {

    private val cached = UserProfileSnapshot(
        name = "老账号",
        avatarUrl = "https://example.invalid/a.png",
        level = 7,
        exp = 1234,
        title = "萌新",
        gender = "m",
        slogan = "签名",
        honorBadges = listOf("badge"),
    )

    @Test
    fun `迁移标记未置位时才迁移`() = runTest {
        val migration = UserProfileCacheMigration { FakePreferencesStore() }

        assertTrue(migration.shouldMigrate(UserProfileSnapshot()))
        // 迁移过一次后 legacyMigrationDone 为 true，后续冷启动不应再进入
        assertFalse(migration.shouldMigrate(cached.copy(legacyMigrationDone = true)))
    }

    /**
     * 登出会清空 name 等业务字段，但不应清空 legacyMigrationDone
     * （见 UserProfileSnapshotDataSource.clear）。这里钉住：即使 name 为空，
     * 只要迁移标记已置位就不再重新触发迁移，避免把上一个账号的旧资料
     * 从 legacy 存储里恢复回来。
     */
    @Test
    fun `登出后name为空但迁移标记仍置位时不再迁移`() = runTest {
        val migration = UserProfileCacheMigration { FakePreferencesStore() }

        val loggedOut = UserProfileSnapshot(legacyMigrationDone = true)
        assertFalse(migration.shouldMigrate(loggedOut))
    }

    @Test
    fun `旧文件里有资料时搬过来并标记迁移完成`() = runTest {
        val legacy = FakePreferencesStore(UserPreferences(profile = cached))
        val migration = UserProfileCacheMigration { legacy }

        val result = migration.migrate(UserProfileSnapshot())
        assertEquals(cached.copy(legacyMigrationDone = true), result)
        assertTrue(result.legacyMigrationDone)
    }

    /** 旧文件没存过资料时不该覆盖业务字段，否则等于白写一次文件；但仍需标记迁移完成。 */
    @Test
    fun `旧文件无资料时保持原值但标记迁移完成`() = runTest {
        val legacy = FakePreferencesStore()
        val migration = UserProfileCacheMigration { legacy }

        val current = UserProfileSnapshot()
        val result = migration.migrate(current)
        assertEquals(current.copy(legacyMigrationDone = true), result)
    }

    /**
     * 旧文件损坏时不能让异常冒出去：那会让新 DataStore 初始化失败，
     * 结果是整个资料缓存不可用，而这只是一份可弃缓存。降级后仍需标记迁移完成，
     * 否则每次冷启动都会重新尝试读这份损坏的旧文件。
     */
    @Test
    fun `旧文件读取失败时降级为空缓存并标记迁移完成`() = runTest {
        val migration = UserProfileCacheMigration { FakePreferencesStore(failOnRead = true) }

        val current = UserProfileSnapshot()
        val result = migration.migrate(current)
        assertEquals(current.copy(legacyMigrationDone = true), result)
    }

    @Test
    fun `清理会把旧字段置空`() = runTest {
        val legacy = FakePreferencesStore(UserPreferences(profile = cached))
        val migration = UserProfileCacheMigration { legacy }

        migration.cleanUp()

        assertEquals(UserProfileSnapshot(), legacy.current.profile)
        assertEquals(1, legacy.writeCount)
    }

    /**
     * 没有旧值时不写盘。shouldMigrate 只看新存储的 name，全新安装每次冷启动都会
     * 走一遍 cleanUp，这里若无条件写回就是每次启动都重写一次偏好文件。
     */
    @Test
    fun `无旧值时不重写偏好文件`() = runTest {
        val legacy = FakePreferencesStore()
        val migration = UserProfileCacheMigration { legacy }

        migration.cleanUp()

        assertEquals(0, legacy.writeCount)
    }

    /** 清理失败只是留了份废数据，不该抛出去打断 DataStore 初始化。 */
    @Test
    fun `清理失败不抛异常`() = runTest {
        val migration = UserProfileCacheMigration { FakePreferencesStore(failOnRead = true) }

        migration.cleanUp()
    }
}

private class FakePreferencesStore(
    initial: UserPreferences = UserPreferences(),
    private val failOnRead: Boolean = false,
) : DataStore<UserPreferences> {

    private val state = MutableStateFlow(initial)

    val current: UserPreferences get() = state.value

    var writeCount = 0
        private set

    override val data: Flow<UserPreferences>
        get() = if (failOnRead) flow { throw IOException("模拟旧文件损坏") } else state

    override suspend fun updateData(
        transform: suspend (t: UserPreferences) -> UserPreferences,
    ): UserPreferences {
        writeCount++
        return transform(state.value).also { state.value = it }
    }
}
