package com.shizq.bika.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSearch(vararg search: Search)

    @Delete
    fun deleteSearch(vararg search: Search)

    @Query("DELETE FROM SEARCH")
    fun deleteAllSearch()

    @get:Query("SELECT DISTINCT TEXT FROM SEARCH ORDER BY ID DESC")
    val allSearchLive: LiveData<List<String>>
}