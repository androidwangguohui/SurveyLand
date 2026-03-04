package com.example.surveyland.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "land_table")
data class LandEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    var villageName: String,      // 乡镇/村名
    val type: String,             // 类型：画地块、走一圈
    val area: Double,             // 面积
    val distance: Double,         // 周长
    val pointsJson: String,       // 多边形坐标集合JSON
    val createTime: Long,        // 创建时间

    val thumbnailPath: String?,   // 本地缩略图路径

    val lat:Double,               // 经度纬度
    val lng:Double,

    val backup:String,            // 备用字段
    val backup2:String,           // 备用字段2
    val backup3:String,           // 备用字段3


)