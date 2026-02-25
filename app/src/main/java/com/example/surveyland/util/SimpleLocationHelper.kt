package com.example.surveyland.util

import android.content.Context
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption

class SimpleLocationHelper(context: Context) {

    private val locationClient = AMapLocationClient(context.applicationContext)
    private val locationOption = AMapLocationClientOption().apply {
        locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        isOnceLocation = true      // 只获取一次
        isNeedAddress = false      // 不需要详细地址
    }

    fun getLocation(onResult: (latitude: Double, longitude: Double) -> Unit) {
        locationClient.setLocationOption(locationOption)
        locationClient.setLocationListener { location ->
            location?.let {
                onResult(it.latitude, it.longitude)
            }
            locationClient.stopLocation() // 获取一次后停止
        }
        locationClient.startLocation()
    }
}