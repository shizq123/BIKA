package com.shizq.bika.core.data.repository

import com.shizq.bika.core.database.dao.TagDao
import com.shizq.bika.core.database.model.TagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagsRepository @Inject constructor(
    private val tagDao: TagDao
) {
    fun getTags(): Flow<List<String>> {
        return tagDao.getAllTags().map { entities ->
            entities.map { it.name }
        }
    }

    suspend fun saveTags(tags: List<String>) {
        val entities = tags.map { TagEntity(it) }
        tagDao.insertAll(entities)
    }
}
