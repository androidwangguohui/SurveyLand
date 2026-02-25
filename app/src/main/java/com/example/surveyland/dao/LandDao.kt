package com.example.surveyland.dao

import androidx.room.*
import com.example.surveyland.entity.LandEntity

@Dao
interface LandDao {

    @Insert
    suspend fun insert(land: LandEntity): Long

    @Query("SELECT * FROM land_table ORDER BY createTime DESC")
    suspend fun getAll(): List<LandEntity>

    @Delete
    suspend fun delete(land: LandEntity)

    @Update
    suspend fun update(land: LandEntity): Int

    // 👇 必须加这个
    @Query("SELECT * FROM land_table WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LandEntity?

}