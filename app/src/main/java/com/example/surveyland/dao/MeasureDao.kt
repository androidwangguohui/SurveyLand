package com.example.surveyland.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.surveyland.entity.MeasureEntity

@Dao
interface MeasureDao {

    @Insert
    suspend fun insert(entity: MeasureEntity)

    @Query("SELECT * FROM measure_table ORDER BY createTime DESC")
    suspend fun getAll(): List<MeasureEntity>
}