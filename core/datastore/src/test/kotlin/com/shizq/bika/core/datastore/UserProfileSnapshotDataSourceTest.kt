package com.shizq.bika.core.datastore

import androidx.datastore.core.DataStore
import com.shizq.bika.core.model.preferences.UserProfileSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [UserProfileSnapshotDataSource.clear] 必须保留 [UserProfileSnapshot.legacyMigrationDone]，
 * 否则登出会让下次冷启动把该字段重置为默认值 false，被 UserProfileCacheMigration 误判为
 * "从未迁移"，进而可能把上一个账号残留在旧存储里的资料快照恢复回来。
 */
class UserProfileSnapshotDataSourceTest {

    @Test
    fun `clear清空业务字段但保留迁移标记`() = runTest {
        val store = FakeSnapshotStore(
            UserProfileSnapshot(
                name = "老账号",
                avatarUrl = "https://example.invalid/a.png",
                level = 7,
                exp = 1234,
                legacyMigrationDone = true,
            ),
        )
        val dataSource = UserProfileSnapshotDataSource(store)

        dataSource.clear()

        val cleared = store.current
        assertEquals(UserProfileSnapshot(legacyMigrationDone = true), cleared)
        assertTrue(cleared.legacyMigrationDone)
        assertEquals("", cleared.name)
    }

    @Test
    fun `迁移标记未置位时clear后仍不置位`() = runTest {
        val store = FakeSnapshotStore(UserProfileSnapshot(name = "老账号"))
        val dataSource = UserProfileSnapshotDataSource(store)

        dataSource.clear()

        assertEquals(UserProfileSnapshot(legacyMigrationDone = false), store.current)
    }
}

private class FakeSnapshotStore(
    initial: UserProfileSnapshot = UserProfileSnapshot(),
) : DataStore<UserProfileSnapshot> {

    val current: UserProfileSnapshot get() = data.value

    override val data: Flow<UserProfileSnapshot>
        field = MutableStateFlow(initial)

    override suspend fun updateData(
        transform: suspend (t: UserProfileSnapshot) -> UserProfileSnapshot,
    ): UserProfileSnapshot {
        return transform(data.value).also { data.value = it }
    }
}
