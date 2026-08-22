package com.shizq.bika.feature.reader.impl.progress

import com.shizq.bika.core.data.paging.ChapterMeta
import com.shizq.bika.feature.reader.impl.ReadingProgressStore

/**
 * 进度保存策略接口
 * 
 * 分离保存逻辑，支持不同场景使用不同策略：
 * - DebounceSaveStrategy: 自动保存（debounce 防抖）
 * - ImmediateSaveStrategy: 紧急保存（立即同步写库）
 */
interface StoreStrategy {
    /**
     * 执行保存操作
     * @return Result.success(Unit) 保存成功, Result.failure(error) 保存失败
     */
    suspend fun store(
        comicId: String,
        chapterOrder: Int,
        meta: ChapterMeta?,
        pageIndex: Int
    ): Result<Unit>
}

/**
 * 立即保存策略（紧急时机用）
 * 
 * 使用场景：
 * - 返回键按下
 * - 应用退后台（ON_STOP）
 * - 组件销毁（onDispose）
 * - ViewModel 销毁（onCleared）
 * 
 * 特点：同步阻塞写库，返回时已落库，适用于进程即将被杀的场景
 */
class ImmediateSaveStrategy(
    private val progressSaver: ReadingProgressStore
) : StoreStrategy {
    override suspend fun store(
        comicId: String,
        chapterOrder: Int,
        meta: ChapterMeta?,
        pageIndex: Int
    ): Result<Unit> {
        return try {
            val success = progressSaver.save(comicId, chapterOrder, meta, pageIndex)
            if (success) Result.success(Unit) else Result.failure(Exception("Save failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 异步保存策略（常规自动保存用）
 * 
 * 使用场景：
 * - 页面变化 debounce 后自动保存
 * 
 * 特点：协程内异步写库，不阻塞主线程
 */
class AsyncSaveStrategy(
    private val progressSaver: ReadingProgressStore
) : StoreStrategy {
    override suspend fun store(
        comicId: String,
        chapterOrder: Int,
        meta: ChapterMeta?,
        pageIndex: Int
    ): Result<Unit> {
        return try {
            val success = progressSaver.saveSuspend(comicId, chapterOrder, meta, pageIndex)
            if (success) Result.success(Unit) else Result.failure(Exception("Save failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
