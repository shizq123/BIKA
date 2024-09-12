package com.shizq.bika.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shizq.bika.database.model.SearchEntity

@Dao
interface SearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(vararg searchEntities: SearchEntity)

    @Delete
    suspend fun deleteSearch(vararg searchEntities: SearchEntity)

    @Query("DELETE FROM SEARCH")
    suspend fun deleteAllSearch()

    @get:Query("SELECT DISTINCT TEXT FROM SEARCH ORDER BY ID DESC")
    val allSearchLive: LiveData<List<String>>
}