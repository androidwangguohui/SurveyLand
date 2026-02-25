package com.example.surveyland.dao

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.surveyland.entity.LandEntity
import com.google.gson.Gson
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.layers.properties.generated.SkyType
import kotlinx.coroutines.launch

object DaoUtils {

    open fun saveLand(context: AppCompatActivity, villageName: String, area: Double, distance: Double, thumbnailPath: String, lat: Double,lng: Double, points: List<Point>, type: Int = 1, id: Long = 0,isType:String) {

        val gson = Gson()
        val pointsJson = gson.toJson(points)



        context.lifecycleScope.launch {
            if(type == 1){
                val land = LandEntity(
                    villageName = villageName,
                    type = isType,
                    area = area,
                    distance = distance,
                    pointsJson = pointsJson,
                    createTime = System.currentTimeMillis(),
                    thumbnailPath =thumbnailPath,
                    lat = lat,
                    lng = lng,
                    backup ="",
                    backup2 ="",
                    backup3 =""
                )
                val newId = AppDatabase.getDatabase(context)
                    .landDao()
                    .insert(land)
                // 插入数据库，获取生成的 id
                Log.e("新地块ID", "$newId") // 👈 这里就是存进去的 id
            }else{
                val land = LandEntity(
                    id = id,
                    villageName = villageName,
                    type = isType,
                    area = area,
                    distance = distance,
                    pointsJson = pointsJson,
                    createTime = System.currentTimeMillis(),
                    thumbnailPath =thumbnailPath,
                    lat = lat,
                    lng = lng,
                    backup ="",
                    backup2 ="",
                    backup3 =""
                )
                val upId = AppDatabase.getDatabase(context)
                    .landDao()
                    .update(land)
                // 插入数据库，获取生成的 id
                Log.e("更新地块ID", "$upId") // 👈 这里就是存进去的 id
            }

        }
    }

}