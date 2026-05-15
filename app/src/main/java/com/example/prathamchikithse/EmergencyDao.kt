package com.example.prathamchikithse

import androidx.room.*

@Dao
interface EmergencyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Emergency>)

    @Query("SELECT * FROM Emergency")
    suspend fun getAll(): List<Emergency>

    @Query("SELECT * FROM Emergency WHERE title = :title LIMIT 1")
    suspend fun getByTitle(title: String): Emergency?

    @Query("DELETE FROM Emergency")
    suspend fun deleteAll()
}