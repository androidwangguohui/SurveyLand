package com.example.map_amap

import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng


object AreaCalculator {

    /** 使用高德官方算法 */
    fun calculate(points: List<LatLng>): Double  {
        if (points.size < 3) return 0.0
        return kotlin.math.abs(AMapUtils.calculateArea(points)).toDouble()
    }
}
