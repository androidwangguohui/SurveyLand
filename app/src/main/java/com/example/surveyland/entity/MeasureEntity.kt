package com.example.surveyland.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measure_table")
data class MeasureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pointsJson: String,
    val distance: Double,
    val area: Double,
    val createTime: Long
)