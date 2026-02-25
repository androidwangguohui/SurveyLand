package com.example.surveyland.util

import com.example.surveyland.entity.LatLngPoint
import com.mapbox.geojson.Point
import kotlin.math.pow

object GeoMeasureUtil {
    private const val R = 6378137.0


    fun area(points: List<Point>): Double {

        if (points.size < 3) return 0.0
        var sum = 0.0
        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]
            sum += Math.toRadians(p2.longitude() - p1.longitude()) *
                    (2 + Math.sin(Math.toRadians(p1.latitude())) + Math.sin(Math.toRadians(p2.latitude())))
        }
        return Math.abs(sum * R * R / 2.0)
    }


    fun distance(a: LatLngPoint, b: LatLngPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = Math.sin(dLat / 2).pow(2) +
                Math.cos(Math.toRadians(a.lat)) * Math.cos(Math.toRadians(b.lat)) *
                Math.sin(dLng / 2).pow(2)
        return 2 * R * Math.asin(Math.sqrt(h))
    }
}